/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2022 Payara Foundation and/or its affiliates. All rights reserved.
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


import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnection;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.StringUtils;

/**
 * @author Steve Millidge (Payara Foundation)
 */
public class AmazonSQSManagedConnection implements ManagedConnection, AmazonSQSConnection {

    private final List<AmazonSQSConnection> connectionHandles = new LinkedList<>();
    private final HashSet<ConnectionEventListener> listeners = new HashSet<>();
    private PrintWriter logWriter;
    private final SqsClient sqsClient;

    AmazonSQSManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo, AmazonSQSManagedConnectionFactory aThis) {
        AwsCredentialsProvider credentialsProvider = getCredentials(aThis);
        sqsClient = SqsClient.builder().region(Region.of(aThis.getRegion())).credentialsProvider(credentialsProvider).build();
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        AmazonSQSConnection newConn = new AmazonSQSConnectionImpl(this);
        connectionHandles.add(newConn);
        return newConn;
    }

    @Override
    public void destroy() throws ResourceException {

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
    public SendMessageResponse sendMessage(SendMessageRequest request) {
        return sqsClient.sendMessage(request);
    }

    @Override
    public SendMessageResponse sendMessage(String queueURL, String messageBody) {
        return sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueURL).messageBody(messageBody).build());
    }

    @Override
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest batch) {
        return sqsClient.sendMessageBatch(batch);
    }

    @Override
    public SendMessageBatchResponse sendMessageBatch(String queueURL, List<SendMessageBatchRequestEntry> entries) {
        return sqsClient.sendMessageBatch(SendMessageBatchRequest.builder().queueUrl(queueURL).entries(entries).build());
    }

    @Override
    public void close() throws Exception {
        destroy();
    }

    private AwsCredentialsProvider getCredentials(AmazonSQSManagedConnectionFactory aThis) {
        AwsCredentialsProvider credentialsProvider;
        if (StringUtils.isNotBlank(aThis.getProfileName())) {
            credentialsProvider = ProfileCredentialsProvider.create(aThis.getProfileName());
        } else if (StringUtils.isNotBlank(aThis.getAwsAccessKeyId()) && StringUtils.isNotBlank(aThis.getAwsSecretKey())) {
            credentialsProvider = new AwsCredentialsProvider(){
                @Override
                public AwsCredentials resolveCredentials() {
                    return new AwsCredentials() {
                        @Override
                        public String accessKeyId() {
                            return aThis.getAwsAccessKeyId();
                        }

                        @Override
                        public String secretAccessKey() {
                            return aThis.getAwsSecretKey();
                        }
                    };
                }
            };
        } else {
            credentialsProvider = DefaultCredentialsProvider.create();
        }
        return credentialsProvider;
    }

}
