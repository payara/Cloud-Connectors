/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2024 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.cloud.connectors.amazonsqs.api.inbound;

import fish.payara.cloud.connectors.amazonsqs.api.OnSQSMessage;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import jakjavaxarta.resource.spi.work.WorkException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * @author Steve Millidge (Payara Foundation)
 */
class SQSPoller extends TimerTask {

    private final AmazonSQSActivationSpec spec;
    private final BootstrapContext ctx;
    private final MessageEndpointFactory factory;
    private final SqsClient client;
    private S3Client s3;
    private static final String S3_BUCKET_NAME = "s3BucketName";
    private static final String S3_KEY = "s3Key";
    private static final Logger LOG = Logger.getLogger(SQSPoller.class.getName());

    SQSPoller(AmazonSQSActivationSpec sqsSpec, BootstrapContext context, MessageEndpointFactory endpointFactory) {
        spec = sqsSpec;
        ctx = context;
        factory = endpointFactory;
        client = SqsClient.builder().region(Region.of(spec.getRegion()))
                .credentialsProvider(spec).build();
        if (spec.getS3BucketName() != null) {
            s3 = S3Client.builder().region(Region.of(spec.getRegion()))
                    .credentialsProvider(spec).build();
        }
    }

    @Override
    public void run() {
        try {
            ReceiveMessageRequest rmr = ReceiveMessageRequest.builder()
                    .queueUrl(spec.getQueueURL())
                    .maxNumberOfMessages(spec.getMaxMessages()).visibilityTimeout(spec.getVisibilityTimeout())
                    .waitTimeSeconds(spec.getPollInterval() / 1000)
                    .attributeNames(Arrays.stream(spec.getAttributeNames().split(","))
                            .map(s -> QueueAttributeName.fromValue(s)).collect(Collectors.toList()))
                    .messageAttributeNames(Arrays.asList(spec.getMessageAttributeNames().split(","))).build();
            List<Message> messages = client.receiveMessage(rmr).messages();
            if (!messages.isEmpty()) {
                Class<?> mdbClass = factory.getEndpointClass();
                for (Message message : messages) {
                    for (Method m : mdbClass.getMethods()) {
                        if (isOnSQSMessageMethod(m) && shouldFetchS3Message(message)) {
                            message = fetchS3MessageContent(message);
                            scheduleSQSWork(m, message);
                        }
                    }
                }
            }
        } catch (IllegalStateException ise) {
            // Fix #29 ensure Illegal State Exception doesn't blow up the timer
            LOG.log(Level.WARNING, "Poller caught an Illegal State Exception", ise);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Poller caught an Unexpected Exception", e);
        }
    }

    private boolean isOnSQSMessageMethod(Method method) {
        return method.isAnnotationPresent(OnSQSMessage.class)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].equals(Message.class);
    }

    private boolean shouldFetchS3Message(Message message) {
        return s3 != null
                && Boolean.TRUE.equals(spec.getS3FetchMessage())
                && message.body().contains(S3_BUCKET_NAME);
    }

    private Message fetchS3MessageContent(Message message) throws IOException {
        Message updatedMessage = message;
        try (JsonReader jsonReader = Json.createReader(new StringReader(message.body()))) {
            JsonArray jsonArray = jsonReader.readArray();
            for (JsonValue jsonValue : jsonArray) {
                if (jsonValue instanceof JsonObject) {
                    JsonObject jsonBody = (JsonObject) jsonValue;
                    String s3BucketName = jsonBody.getString(S3_BUCKET_NAME);
                    String s3Key = jsonBody.getString(S3_KEY);
                    LOG.log(Level.FINE, "S3 object received, S3 bucket name: {0}, S3 object key:{1}", new Object[]{s3BucketName, s3Key});
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(s3BucketName).key(s3Key).build();
                    ResponseInputStream<GetObjectResponse> responseInputStream = s3.getObject(getObjectRequest);
                    String content;
                    try {
                        content = IoUtils.toUtf8String(responseInputStream);
                        updatedMessage = Message.builder()
                                .attributes(message.attributes())
                                .body(content)
                                .md5OfBody(message.md5OfBody())
                                .messageAttributes(message.messageAttributes())
                                .messageId(message.messageId())
                                .receiptHandle(message.receiptHandle())
                                .build();
                    } finally {
                        responseInputStream.close();
                    }
                }
            }
        } catch (JsonException e) {
            LOG.log(Level.WARNING, "Error parsing S3 message metadata JSON", e);
        }
        return updatedMessage;
    }

    private void scheduleSQSWork(Method method, Message message) {
        try {
            ctx.getWorkManager().scheduleWork(new SQSWork(client, factory, method, message, spec.getQueueURL()));
        } catch (WorkException ex) {
            Logger.getLogger(AmazonSQSResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
