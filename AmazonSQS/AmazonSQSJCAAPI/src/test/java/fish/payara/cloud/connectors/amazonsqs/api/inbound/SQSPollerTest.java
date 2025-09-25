/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SQSPollerTest {

    private AmazonSQSActivationSpec spec;
    private BootstrapContext ctx;
    private MessageEndpointFactory factory;
    private SqsClient sqsClient;
    private S3Client s3Client;
    private SQSPoller poller;

    @BeforeEach
    void setUp() {
        spec = mock(AmazonSQSActivationSpec.class);
        ctx = mock(BootstrapContext.class);
        factory = mock(MessageEndpointFactory.class);
        sqsClient = mock(SqsClient.class);
        s3Client = mock(S3Client.class);

        when(spec.getRegion()).thenReturn("us-east-1");
        when(spec.getQueueURL()).thenReturn("queue-url");
        when(spec.getMaxMessages()).thenReturn(1);
        when(spec.getVisibilityTimeout()).thenReturn(30);
        when(spec.getPollInterval()).thenReturn(1000);
        when(spec.getAttributeNames()).thenReturn("All");
        when(spec.getMessageAttributeNames()).thenReturn("All");
    }

    @Test
    void testFetchS3MessageContentReturnsUpdatedMessage() throws Exception {
        // Prepare JSON body with S3 info
        JsonObjectBuilder objBuilder = Json.createObjectBuilder()
                .add("s3BucketName", "bucket")
                .add("s3Key", "key");
        JsonArrayBuilder arrBuilder = Json.createArrayBuilder().add(objBuilder);
        StringWriter sw = new StringWriter();
        try (JsonWriter writer = Json.createWriter(sw)) {
            writer.writeArray(arrBuilder.build());
        }
        String jsonBody = sw.toString();

        // Mock S3 client and response
        S3Client s3 = mock(S3Client.class);
        String s3Content = "file-content";
        InputStream is = new ByteArrayInputStream(s3Content.getBytes());
        ResponseInputStream<GetObjectResponse> responseInputStream = mock(ResponseInputStream.class);
        when(responseInputStream.readAllBytes()).thenReturn(s3Content.getBytes());
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        // Build original message
        Message origMsg = Message.builder()
                .body(jsonBody)
                .messageId("mid")
                .receiptHandle("rh")
                .build();

        // Use reflection to inject s3 client into poller
        poller = new SQSPoller(spec, ctx, factory);
        var s3Field = SQSPoller.class.getDeclaredField("s3");
        s3Field.setAccessible(true);
        s3Field.set(poller, s3);

        // Mock IoUtils.toUtf8String
        try (var mockedIoUtils = org.mockito.Mockito.mockStatic(software.amazon.awssdk.utils.IoUtils.class)) {
            mockedIoUtils.when(() -> software.amazon.awssdk.utils.IoUtils.toUtf8String(responseInputStream))
                    .thenReturn(s3Content);

            var method = SQSPoller.class.getDeclaredMethod("fetchS3MessageContent", Message.class);
            method.setAccessible(true);
            Message updated = (Message) method.invoke(poller, origMsg);

            assertNotNull(updated);
            assertEquals(s3Content, updated.body());
            assertEquals(origMsg.messageId(), updated.messageId());
            assertEquals(origMsg.receiptHandle(), updated.receiptHandle());
        }
    }

    @Test
    void testFetchS3MessageContentHandlesJsonException() throws Exception {
        // Invalid JSON body
        String invalidJson = "not a json";
        Message origMsg = Message.builder().body(invalidJson).build();

        poller = new SQSPoller(spec, ctx, factory);

        // Should not throw, should return original message
        var method = SQSPoller.class.getDeclaredMethod("fetchS3MessageContent", Message.class);
        method.setAccessible(true);
        Message result = (Message) method.invoke(poller, origMsg);
        assertEquals(origMsg, result);
    }

    @Test
    void testShouldFetchS3MessageReturnsTrue() throws Exception {
        poller = new SQSPoller(spec, ctx, factory);
        var s3Field = SQSPoller.class.getDeclaredField("s3");
        s3Field.setAccessible(true);
        s3Field.set(poller, mock(S3Client.class));

        when(spec.getS3FetchMessage()).thenReturn(true);

        Message msg = Message.builder().body("s3BucketName").build();
        var method = SQSPoller.class.getDeclaredMethod("shouldFetchS3Message", Message.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(poller, msg);
        assertTrue(result);
    }

    @Test
    void testShouldFetchS3MessageReturnsFalseIfNoS3() throws Exception {
        poller = new SQSPoller(spec, ctx, factory);
        when(spec.getS3FetchMessage()).thenReturn(true);

        Message msg = Message.builder().body("s3BucketName").build();
        var method = SQSPoller.class.getDeclaredMethod("shouldFetchS3Message", Message.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(poller, msg);
        assertFalse(result);
    }

    @Test
    void testIsOnSQSMessageMethod() throws Exception {
        poller = new SQSPoller(spec, ctx, factory);

        class Dummy {
            @fish.payara.cloud.connectors.amazonsqs.api.OnSQSMessage
            public void handle(software.amazon.awssdk.services.sqs.model.Message msg) {}
            public void notHandle(String s) {}
        }
        Method m1 = Dummy.class.getMethod("handle", software.amazon.awssdk.services.sqs.model.Message.class);
        Method m2 = Dummy.class.getMethod("notHandle", String.class);

        var method = SQSPoller.class.getDeclaredMethod("isOnSQSMessageMethod", Method.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(poller, m1));
        assertFalse((boolean) method.invoke(poller, m2));
    }
}