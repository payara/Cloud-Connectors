/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnection;
import fish.payara.cloud.connectors.amazonsqs.api.AwsCredentialsProviderUtils;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Steve Millidge (Payara Foundation)
 */
public class AmazonSQSManagedConnection implements ManagedConnection, AmazonSQSConnection {

    private final List<AmazonSQSConnection> connectionHandles = new LinkedList<>();
    private final HashSet<ConnectionEventListener> listeners = new HashSet<>();
    private PrintWriter logWriter;
    private final AmazonSQS sqsClient;

    AmazonSQSManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo, AmazonSQSManagedConnectionFactory aThis) {

        AWSCredentialsProvider credentialsProvider = AwsCredentialsProviderUtils.getProvider(
                aThis.getAwsAccessKeyId(),
                aThis.getAwsSecretKey(),
                aThis.getAwsSessionToken(),
                aThis.getProfileName());

        sqsClient = AmazonSQSClientBuilder.standard().withRegion(aThis.getRegion()).withCredentials(credentialsProvider).build();
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        AmazonSQSConnection newConn = new AmazonSQSConnectionImpl(this);
        connectionHandles.add(newConn);
        return newConn;
    }

    @Override
    public void destroy() throws ResourceException {
        sqsClient.shutdown();
    }

    @Override
    public void cleanup() throws ResourceException {
        connectionHandles.clear();
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        AmazonSQSConnectionImpl impl = (AmazonSQSConnectionImpl) connection;
        impl.setRealConnection(this);
        connectionHandles.add(impl);
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("Not supported yet.");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Not supported yet.");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() throws ResourceException {
                return "Amazon SQS JCA Adapter";
            }

            @Override
            public String getEISProductVersion() throws ResourceException {
                return "1.0.0";
            }

            @Override
            public int getMaxConnections() throws ResourceException {
                return 0;
            }

            @Override
            public String getUserName() throws ResourceException {
                return "anonymous";
            }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    void removeHandle(AmazonSQSConnection connection) {
        connectionHandles.remove(connection);
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connection);
        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }

    @Override
    public SendMessageResult sendMessage(SendMessageRequest request) {
        return sqsClient.sendMessage(request);
    }

    @Override
    public SendMessageResult sendMessage(String queueURL, String messageBody) {
        return sqsClient.sendMessage(queueURL, messageBody);
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(String queueURL, List<SendMessageBatchRequestEntry> entries) {
        return sqsClient.sendMessageBatch(queueURL, entries);
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest batch) {
        return sqsClient.sendMessageBatch(batch);
    }

    @Override
    public void close() throws Exception {
        destroy();
    }

}
