/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.cloud.connectors.amazonsqs.api.outbound;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import org.testng.annotations.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.IoUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * Reproduces FISH-8467: receiving a message larger than the SQS 256KB limit
 * (i.e. one that was offloaded to S3 by the extended client on send) used to
 * fail/return the raw S3 pointer JSON because receiveMessage went straight to
 * the plain {@link SqsClient} instead of the {@link AmazonSQSExtendedClient}.
 * <p>
 * These tests stand in a fake in-memory SQS queue and S3 bucket (no AWS
 * account, credentials, or Docker/LocalStack required) behind a real
 * {@link AmazonSQSExtendedClient}, and exercise it exactly the way
 * {@link AmazonSQSManagedConnection#receiveMessage} now does.
 */
public class LargeMessageReceiveTest {

    private static final String QUEUE_URL = "https://sqs.fake/queue";
    private static final String BUCKET = "fake-bucket";
    private static final int THRESHOLD = 1024; // small threshold so tests stay fast

    @Test
    public void receiveMessageThroughExtendedClientReturnsOriginalLargePayload() {
        InMemorySqsClient fakeSqs = new InMemorySqsClient();
        InMemoryS3Client fakeS3 = new InMemoryS3Client();
        AmazonSQSExtendedClient sqsExtClient = extendedClient(fakeSqs, fakeS3);

        String largeBody = "x".repeat(THRESHOLD * 4);
        sqsExtClient.sendMessage(SendMessageRequest.builder().queueUrl(QUEUE_URL).messageBody(largeBody).build());

        // Sanity check: the queue itself only holds the S3 pointer, not the payload -
        // this is what the old sqsClient.receiveMessage() call used to hand back.
        String rawBodyStoredInQueue = fakeSqs.peekOnlyMessage().body();
        assertNotEquals(rawBodyStoredInQueue, largeBody, "raw SQS body should be the S3 pointer, not the payload");
        fakeSqs.reset();
        sqsExtClient.sendMessage(SendMessageRequest.builder().queueUrl(QUEUE_URL).messageBody(largeBody).build());

        ReceiveMessageResponse response = sqsExtClient.receiveMessage(
                ReceiveMessageRequest.builder().queueUrl(QUEUE_URL).maxNumberOfMessages(1).build());

        assertEquals(response.messages().size(), 1);
        assertEquals(response.messages().get(0).body(), largeBody, "receiveMessage should transparently resolve the S3-backed payload");
    }

    @Test
    public void receiveMessageThroughExtendedClientLeavesSmallPayloadUnchanged() {
        InMemorySqsClient fakeSqs = new InMemorySqsClient();
        InMemoryS3Client fakeS3 = new InMemoryS3Client();
        AmazonSQSExtendedClient sqsExtClient = extendedClient(fakeSqs, fakeS3);

        String smallBody = "hello world";
        sqsExtClient.sendMessage(SendMessageRequest.builder().queueUrl(QUEUE_URL).messageBody(smallBody).build());

        ReceiveMessageResponse response = sqsExtClient.receiveMessage(
                (Consumer<ReceiveMessageRequest.Builder>) builder -> builder.queueUrl(QUEUE_URL).maxNumberOfMessages(1));

        assertEquals(response.messages().size(), 1);
        assertEquals(response.messages().get(0).body(), smallBody, "small messages must pass through unmodified");
        assertEquals(fakeS3.objectCount(), 0, "small messages must never be offloaded to S3");
    }

    private static AmazonSQSExtendedClient extendedClient(SqsClient sqsClient, S3Client s3Client) {
        ExtendedClientConfiguration config = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(s3Client, BUCKET)
                .withPayloadSizeThreshold(THRESHOLD);
        return new AmazonSQSExtendedClient(sqsClient, config);
    }

    /** Minimal in-memory stand-in for a single SQS queue. */
    private static class InMemorySqsClient implements SqsClient {

        private final Deque<Message> queue = new ArrayDeque<>();

        @Override
        public SendMessageResponse sendMessage(SendMessageRequest request) {
            Message message = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .receiptHandle(UUID.randomUUID().toString())
                    .body(request.messageBody())
                    .messageAttributes(request.messageAttributes())
                    .build();
            queue.addLast(message);
            return SendMessageResponse.builder().messageId(message.messageId()).build();
        }

        @Override
        public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest request) {
            int max = request.maxNumberOfMessages() == null ? 1 : request.maxNumberOfMessages();
            List<Message> received = new ArrayList<>();
            while (received.size() < max && !queue.isEmpty()) {
                received.add(queue.pollFirst());
            }
            return ReceiveMessageResponse.builder().messages(received).build();
        }

        Message peekOnlyMessage() {
            return queue.peekFirst();
        }

        void reset() {
            queue.clear();
        }

        @Override
        public String serviceName() {
            return SqsClient.SERVICE_NAME;
        }

        @Override
        public void close() {
        }
    }

    /** Minimal in-memory stand-in for a single S3 bucket. */
    private static class InMemoryS3Client implements S3Client {

        private final Map<String, byte[]> objects = new HashMap<>();

        @Override
        public PutObjectResponse putObject(PutObjectRequest request, RequestBody requestBody) {
            try (InputStream in = requestBody.contentStreamProvider().newStream()) {
                objects.put(key(request.bucket(), request.key()), IoUtils.toByteArray(in));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return PutObjectResponse.builder().build();
        }

        @Override
        public ResponseInputStream<GetObjectResponse> getObject(GetObjectRequest request) {
            byte[] content = objects.get(key(request.bucket(), request.key()));
            return new ResponseInputStream<>(GetObjectResponse.builder().build(), new ByteArrayInputStream(content));
        }

        @Override
        public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
            objects.remove(key(request.bucket(), request.key()));
            return DeleteObjectResponse.builder().build();
        }

        int objectCount() {
            return objects.size();
        }

        private static String key(String bucket, String key) {
            return bucket + "/" + key;
        }

        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
        }
    }
}
