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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import fish.payara.cloud.connectors.amazonsqs.api.OnSQSMessage;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkException;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
class SQSPoller extends TimerTask {

    private final AmazonSQSActivationSpec spec;
    private final BootstrapContext ctx;
    private final MessageEndpointFactory factory;
    private final AmazonSQS client;
    private AmazonS3 s3;
    private static final String S3_BUCKET_NAME = "s3BucketName";
    private static final String S3_KEY = "s3Key";
    private static final Logger LOG = Logger.getLogger(SQSPoller.class.getName());

    SQSPoller(AmazonSQSActivationSpec sqsSpec, BootstrapContext context, MessageEndpointFactory endpointFactory) {
        spec = sqsSpec;
        ctx = context;
        factory = endpointFactory;
        client = AmazonSQSClientBuilder.standard().withCredentials(spec).withRegion(spec.getRegion()).build();
        if (spec.getS3BucketName() != null) {
            s3 = AmazonS3ClientBuilder.standard().withCredentials(spec).withRegion(spec.getRegion()).build();
        }
    }

    @Override
    public void run() {
        try {
            ReceiveMessageRequest rmr = new ReceiveMessageRequest(spec.getQueueURL());
            rmr.setMaxNumberOfMessages(spec.getMaxMessages());
            rmr.setVisibilityTimeout(spec.getVisibilityTimeout());
            rmr.setWaitTimeSeconds(spec.getPollInterval() / 1000);
            rmr.setAttributeNames(Arrays.asList(spec.getAttributeNames().split(",")));
            rmr.setMessageAttributeNames(Arrays.asList(spec.getMessageAttributeNames().split(",")));
            ReceiveMessageResult rmResult = client.receiveMessage(rmr);
            if (!rmResult.getMessages().isEmpty()) {
                Class<?> mdbClass = factory.getEndpointClass();
                for (Message message : rmResult.getMessages()) {
                    for (Method m : mdbClass.getMethods()) {
                        if (isOnSQSMessageMethod(m) && shouldFetchS3Message(message)) {
                            fetchS3MessageContent(message);
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
                && message.getBody().contains(S3_BUCKET_NAME);
    }

    private void fetchS3MessageContent(Message message) throws IOException {
        try (JsonReader jsonReader = Json.createReader(new StringReader(message.getBody()))) {
            JsonArray jsonArray = jsonReader.readArray();
            for (JsonValue jsonValue : jsonArray) {
                if (jsonValue instanceof JsonObject) {
                    JsonObject jsonBody = (JsonObject) jsonValue;
                    String s3BucketName = jsonBody.getString(S3_BUCKET_NAME);
                    String s3Key = jsonBody.getString(S3_KEY);
                    LOG.log(Level.FINE, "S3 object received, S3 bucket name: {0}, S3 object key:{1}", new Object[]{s3BucketName, s3Key});
                    GetObjectRequest getObjectRequest = new GetObjectRequest(s3BucketName, s3Key);
                    S3Object s3Object = s3.getObject(getObjectRequest);
                    try (S3ObjectInputStream objectInputStream = s3Object.getObjectContent()) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        StringBuilder content = new StringBuilder();
                        while ((bytesRead = objectInputStream.read(buffer)) != -1) {
                            content.append(new String(buffer, 0, bytesRead));
                        }
                        message.setBody(content.toString());
                    }
                }
            }
        } catch (JsonException e) {
            LOG.log(Level.WARNING, "Error parsing S3 message metadata JSON", e);
        }
    }

    private void scheduleSQSWork(Method method, Message message) {
        try {
            ctx.getWorkManager().scheduleWork(new SQSWork(client, factory, method, message, spec.getQueueURL()));
        } catch (WorkException ex) {
            Logger.getLogger(AmazonSQSResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void stop() {
        client.shutdown();
    }

}
