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
package fish.payara.cloud.connectors.amazonsqs.api.outbound;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AddPermissionRequest;
import com.amazonaws.services.sqs.model.AddPermissionResult;
import com.amazonaws.services.sqs.model.CancelMessageMoveTaskRequest;
import com.amazonaws.services.sqs.model.CancelMessageMoveTaskResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesResult;
import com.amazonaws.services.sqs.model.ListMessageMoveTasksRequest;
import com.amazonaws.services.sqs.model.ListMessageMoveTasksResult;
import com.amazonaws.services.sqs.model.ListQueueTagsRequest;
import com.amazonaws.services.sqs.model.ListQueueTagsResult;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.PurgeQueueResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.RemovePermissionRequest;
import com.amazonaws.services.sqs.model.RemovePermissionResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesResult;
import com.amazonaws.services.sqs.model.StartMessageMoveTaskRequest;
import com.amazonaws.services.sqs.model.StartMessageMoveTaskResult;
import com.amazonaws.services.sqs.model.TagQueueRequest;
import com.amazonaws.services.sqs.model.TagQueueResult;
import com.amazonaws.services.sqs.model.UntagQueueRequest;
import com.amazonaws.services.sqs.model.UntagQueueResult;
import com.amazonaws.util.StringUtils;
import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnection;

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
import java.util.Map;

/**
 * @author Steve Millidge (Payara Foundation)
 */
public class AmazonSQSManagedConnection implements ManagedConnection, AmazonSQSConnection {

    private final List<AmazonSQSConnection> connectionHandles = new LinkedList<>();
    private final HashSet<ConnectionEventListener> listeners = new HashSet<>();
    private PrintWriter logWriter;
    private final AmazonSQS sqsClient;

    AmazonSQSManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo, AmazonSQSManagedConnectionFactory aThis) {

        AWSCredentialsProvider credentialsProvider = getCredentials(aThis);
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
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        return sqsClient.receiveMessage(receiveMessageRequest);
    }

    @Override
    public ReceiveMessageResult receiveMessage(String queueUrl) {
        return sqsClient.receiveMessage(queueUrl);
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest) {
        return sqsClient.getQueueAttributes(getQueueAttributesRequest);
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(String queueUrl, List<String> attributeNames) {
        return sqsClient.getQueueAttributes(queueUrl, attributeNames);
    }

    @Override
    public SetQueueAttributesResult setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) {
        return sqsClient.setQueueAttributes(setQueueAttributesRequest);
    }

    @Override
    public SetQueueAttributesResult setQueueAttributes(String queueUrl, Map<String, String> attributes) {
        return sqsClient.setQueueAttributes(queueUrl, attributes);
    }

    @Override
    public CreateQueueResult createQueue(CreateQueueRequest createQueueRequest) {
        return sqsClient.createQueue(createQueueRequest);
    }

    @Override
    public CreateQueueResult createQueue(String queueName) {
        return sqsClient.createQueue(queueName);
    }

    @Override
    public DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        return sqsClient.deleteMessage(deleteMessageRequest);
    }

    @Override
    public DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle) {
        return sqsClient.deleteMessage(queueUrl, receiptHandle);
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {
        return sqsClient.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries) {
        return sqsClient.deleteMessageBatch(queueUrl, entries);
    }

    @Override
    public DeleteQueueResult deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        return sqsClient.deleteQueue(deleteQueueRequest);
    }

    @Override
    public DeleteQueueResult deleteQueue(String queueUrl) {
        return sqsClient.deleteQueue(queueUrl);
    }

    @Override
    public GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        return sqsClient.getQueueUrl(getQueueUrlRequest);
    }

    @Override
    public GetQueueUrlResult getQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(queueName);
    }

    @Override
    public ListQueueTagsResult listQueueTags(ListQueueTagsRequest listQueueTagsRequest) {
        return sqsClient.listQueueTags(listQueueTagsRequest);
    }

    @Override
    public ListQueueTagsResult listQueueTags(String queueUrl) {
        return sqsClient.listQueueTags(queueUrl);
    }

    @Override
    public ListQueuesResult listQueues(ListQueuesRequest listQueuesRequest) {
        return sqsClient.listQueues(listQueuesRequest);
    }

    @Override
    public ListQueuesResult listQueues() {
        return sqsClient.listQueues();
    }

    @Override
    public ListQueuesResult listQueues(String queueNamePrefix) {
        return sqsClient.listQueues(queueNamePrefix);
    }

    @Override
    public PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        return sqsClient.purgeQueue(purgeQueueRequest);
    }

    @Override
    public TagQueueResult tagQueue(TagQueueRequest tagQueueRequest) {
        return sqsClient.tagQueue(tagQueueRequest);
    }

    @Override
    public TagQueueResult tagQueue(String queueUrl, Map<String, String> tags) {
        return sqsClient.tagQueue(queueUrl, tags);
    }

    @Override
    public UntagQueueResult untagQueue(UntagQueueRequest untagQueueRequest) {
        return sqsClient.untagQueue(untagQueueRequest);
    }

    @Override
    public UntagQueueResult untagQueue(String queueUrl, List<String> tagKeys) {
        return sqsClient.untagQueue(queueUrl, tagKeys);
    }

    public AddPermissionResult addPermission(AddPermissionRequest addPermissionRequest) {
        return sqsClient.addPermission(addPermissionRequest);
    }

    public AddPermissionResult addPermission(String queueUrl, String label, List<String> awsAccountIds, List<String> actions) {
        return sqsClient.addPermission(queueUrl, label, awsAccountIds, actions);
    }

    public CancelMessageMoveTaskResult cancelMessageMoveTask(CancelMessageMoveTaskRequest cancelMessageMoveTaskRequest) {
        return sqsClient.cancelMessageMoveTask(cancelMessageMoveTaskRequest);
    }

    public ChangeMessageVisibilityResult changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {
        return sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    public ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl, String receiptHandle, Integer visibilityTimeout) {
        return sqsClient.changeMessageVisibility(queueUrl, receiptHandle, visibilityTimeout);
    }

    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
        return sqsClient.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(String queueUrl, List<ChangeMessageVisibilityBatchRequestEntry> entries) {
        return sqsClient.changeMessageVisibilityBatch(queueUrl, entries);
    }

    public ListDeadLetterSourceQueuesResult listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) {
        return sqsClient.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    public ListMessageMoveTasksResult listMessageMoveTasks(ListMessageMoveTasksRequest listMessageMoveTasksRequest) {
        return sqsClient.listMessageMoveTasks(listMessageMoveTasksRequest);
    }

    public RemovePermissionResult removePermission(RemovePermissionRequest removePermissionRequest) {
        return sqsClient.removePermission(removePermissionRequest);
    }

    public RemovePermissionResult removePermission(String queueUrl, String label) {
        return sqsClient.removePermission(queueUrl, label);
    }

    public StartMessageMoveTaskResult startMessageMoveTask(StartMessageMoveTaskRequest startMessageMoveTaskRequest) {
        return sqsClient.startMessageMoveTask(startMessageMoveTaskRequest);
    }

    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
        return sqsClient.getCachedResponseMetadata(amazonWebServiceRequest);
    }

    @Override
    public void close() throws Exception {
        destroy();
    }

    private AWSCredentialsProvider getCredentials(AmazonSQSManagedConnectionFactory aThis) {
        AWSCredentialsProvider credentialsProvider;
        if (!StringUtils.isNullOrEmpty(aThis.getRoleArn())) {
            credentialsProvider = STSCredentialsProvider.create(aThis.getRoleArn(), aThis.getRoleSessionName(), Regions.fromName(aThis.getRegion()));
        } else if (!StringUtils.isNullOrEmpty(aThis.getProfileName())) {
            credentialsProvider = new ProfileCredentialsProvider(aThis.getProfileName());
        } else if (!StringUtils.isNullOrEmpty(aThis.getAwsAccessKeyId()) && !StringUtils.isNullOrEmpty(aThis.getAwsSecretKey())) {
            credentialsProvider = new AWSStaticCredentialsProvider(new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return aThis.getAwsAccessKeyId();
                }

                @Override
                public String getAWSSecretKey() {
                    return aThis.getAwsSecretKey();
                }
            });
        } else {
            credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
        }
        return credentialsProvider;
    }

}
