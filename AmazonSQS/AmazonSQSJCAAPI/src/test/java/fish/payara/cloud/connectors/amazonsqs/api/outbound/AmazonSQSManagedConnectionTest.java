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
package fish.payara.cloud.connectors.amazonsqs.api.outbound;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnection;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnectionMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.security.auth.Subject;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AmazonSQSManagedConnectionTest {

    private AmazonSQSManagedConnectionFactory factory;
    private Subject subject;
    private ConnectionRequestInfo cxRequestInfo;

    @BeforeEach
    void setUp() {
        factory = mock(AmazonSQSManagedConnectionFactory.class);
        when(factory.getRegion()).thenReturn("us-east-1");
        when(factory.getAwsAccessKeyId()).thenReturn("access");
        when(factory.getAwsSecretKey()).thenReturn("secret");
        when(factory.getProfileName()).thenReturn(null);
        when(factory.getRoleArn()).thenReturn(null);
        when(factory.getRoleSessionName()).thenReturn(null);
        when(factory.getS3BucketName()).thenReturn(null);
        when(factory.getS3SizeThreshold()).thenReturn(null);
        when(factory.getS3KeyPrefix()).thenReturn(null);

        subject = new Subject();
        cxRequestInfo = mock(ConnectionRequestInfo.class);
    }

    @Test
    void testGetConnectionAddsHandle() throws ResourceException {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        AmazonSQSConnection handle = (AmazonSQSConnection) conn.getConnection(subject, cxRequestInfo);
        assertNotNull(handle);
    }

    @Test
    void testCleanupClearsHandles() throws ResourceException {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        conn.getConnection(subject, cxRequestInfo);
        conn.cleanup();
        // No exception means success; internal handles list is cleared
    }

    @Test
    void testAssociateConnectionAddsHandle() throws ResourceException {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        AmazonSQSConnectionImpl mockConn = mock(AmazonSQSConnectionImpl.class);
        conn.associateConnection(mockConn);
        verify(mockConn).setRealConnection(conn);
    }

    @Test
    void testAddAndRemoveConnectionEventListener() {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        conn.addConnectionEventListener(listener);
        conn.removeConnectionEventListener(listener);
        // No exception means listeners set is managed
    }

    @Test
    void testRemoveHandleFiresEvent() {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        conn.addConnectionEventListener(listener);
        AmazonSQSConnectionImpl mockConn = mock(AmazonSQSConnectionImpl.class);
        conn.removeHandle(mockConn);
        verify(listener).connectionClosed(any(ConnectionEvent.class));
    }

    @Test
    void testGetMetaData() throws ResourceException {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        ManagedConnectionMetaData meta = conn.getMetaData();
        assertEquals("Amazon SQS JCA Adapter", meta.getEISProductName());
        assertEquals("1.0.0", meta.getEISProductVersion());
        assertEquals(0, meta.getMaxConnections());
        assertEquals("anonymous", meta.getUserName());
    }

    @Test
    void testSetAndGetLogWriter() throws ResourceException {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        PrintWriter pw = new PrintWriter(System.out);
        conn.setLogWriter(pw);
        assertEquals(pw, conn.getLogWriter());
    }

    @Test
    void testXAResourceAndLocalTransactionNotSupported() {
        AmazonSQSManagedConnection conn = new AmazonSQSManagedConnection(subject, cxRequestInfo, factory);
        assertThrows(NotSupportedException.class, conn::getXAResource);
        assertThrows(NotSupportedException.class, conn::getLocalTransaction);
    }

    @Test
    void testSendMessageDelegatesToCorrectClient() {
        AmazonSQSManagedConnectionFactory fac = mock(AmazonSQSManagedConnectionFactory.class);
        when(fac.getRegion()).thenReturn("us-east-1");
        when(fac.getAwsAccessKeyId()).thenReturn("access");
        when(fac.getAwsSecretKey()).thenReturn("secret");
        when(fac.getProfileName()).thenReturn(null);
        when(fac.getRoleArn()).thenReturn(null);
        when(fac.getRoleSessionName()).thenReturn(null);
        when(fac.getS3BucketName()).thenReturn(null);
        when(fac.getS3SizeThreshold()).thenReturn(null);
        when(fac.getS3KeyPrefix()).thenReturn(null);

        AmazonSQSManagedConnection conn = spy(new AmazonSQSManagedConnection(subject, cxRequestInfo, fac));
        SendMessageRequest req = SendMessageRequest.builder().queueUrl("url").messageBody("short").build();
        //doReturn(false).when(conn).isLargeMessage(anyString());

        // Mock SqsClient and AmazonSQSExtendedClient
        SqsClient sqsClient = mock(SqsClient.class);
        AmazonSQSExtendedClient extClient = mock(AmazonSQSExtendedClient.class);
        // Use reflection to set private fields
        try {
            var sqsField = AmazonSQSManagedConnection.class.getDeclaredField("sqsClient");
            sqsField.setAccessible(true);
            sqsField.set(conn, sqsClient);
            var extField = AmazonSQSManagedConnection.class.getDeclaredField("sqsExtClient");
            extField.setAccessible(true);
            extField.set(conn, extClient);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        SendMessageResponse resp = SendMessageResponse.builder().messageId("id").build();
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(resp);

        SendMessageResponse result = conn.sendMessage(req);
        assertEquals("id", result.messageId());
        verify(sqsClient).sendMessage(req);
        verify(extClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testSendMessageLargeDelegatesToExtendedClient() {
        AmazonSQSManagedConnectionFactory fac = mock(AmazonSQSManagedConnectionFactory.class);
        when(fac.getRegion()).thenReturn("us-east-1");
        when(fac.getAwsAccessKeyId()).thenReturn("access");
        when(fac.getAwsSecretKey()).thenReturn("secret");
        when(fac.getProfileName()).thenReturn(null);
        when(fac.getRoleArn()).thenReturn(null);
        when(fac.getRoleSessionName()).thenReturn(null);
        when(fac.getS3BucketName()).thenReturn(null);
        when(fac.getS3SizeThreshold()).thenReturn(null);
        when(fac.getS3KeyPrefix()).thenReturn(null);

        // Generate a string > 256KB to be considered a large message
        int size = 262145;
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('A');
        }
        String largeMessage = sb.toString();

        AmazonSQSManagedConnection conn = spy(new AmazonSQSManagedConnection(subject, cxRequestInfo, fac));
        SendMessageRequest req = SendMessageRequest.builder().queueUrl("url").messageBody(largeMessage).build();

        SqsClient sqsClient = mock(SqsClient.class);
        AmazonSQSExtendedClient extClient = mock(AmazonSQSExtendedClient.class);
        try {
            var sqsField = AmazonSQSManagedConnection.class.getDeclaredField("sqsClient");
            sqsField.setAccessible(true);
            sqsField.set(conn, sqsClient);
            var extField = AmazonSQSManagedConnection.class.getDeclaredField("sqsExtClient");
            extField.setAccessible(true);
            extField.set(conn, extClient);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        SendMessageResponse resp = SendMessageResponse.builder().messageId("id2").build();
        when(extClient.sendMessage(any(SendMessageRequest.class))).thenReturn(resp);

        SendMessageResponse result = conn.sendMessage(req);
        assertEquals("id2", result.messageId());
        verify(extClient).sendMessage(req);
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testCloseCallsDestroy() throws Exception {
        AmazonSQSManagedConnection conn = spy(new AmazonSQSManagedConnection(subject, cxRequestInfo, factory));
        conn.close();
        verify(conn).destroy();
    }
}