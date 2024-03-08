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

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
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
import java.util.function.Consumer;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.CancelMessageMoveTaskRequest;
import software.amazon.awssdk.services.sqs.model.CancelMessageMoveTaskResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ListMessageMoveTasksRequest;
import software.amazon.awssdk.services.sqs.model.ListMessageMoveTasksResponse;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsRequest;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.RemovePermissionResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.StartMessageMoveTaskRequest;
import software.amazon.awssdk.services.sqs.model.StartMessageMoveTaskResponse;
import software.amazon.awssdk.services.sqs.model.TagQueueRequest;
import software.amazon.awssdk.services.sqs.model.TagQueueResponse;
import software.amazon.awssdk.services.sqs.model.UntagQueueRequest;
import software.amazon.awssdk.services.sqs.model.UntagQueueResponse;
import software.amazon.awssdk.utils.StringUtils;

/**
 * @author Steve Millidge (Payara Foundation)
 */
public class AmazonSQSManagedConnection implements ManagedConnection, AmazonSQSConnection {

    private final List<AmazonSQSConnection> connectionHandles = new LinkedList<>();
    private final HashSet<ConnectionEventListener> listeners = new HashSet<>();
    private PrintWriter logWriter;
    private final SqsClient sqsClient;
    private final AmazonSQSExtendedClient sqsExtClient;
    
    AmazonSQSManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo, AmazonSQSManagedConnectionFactory aThis) {
        AwsCredentialsProvider credentialsProvider = getCredentials(aThis);
        sqsClient = SqsClient.builder().region(Region.of(aThis.getRegion())).credentialsProvider(credentialsProvider).build();

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration();
        if (aThis.getS3BucketName() != null) {
            final S3Client s3 = S3Client.builder().region(Region.of(aThis.getRegion())).credentialsProvider(credentialsProvider).build();
            extendedClientConfig = extendedClientConfig.withPayloadSupportEnabled(s3, aThis.getS3BucketName());
            if (aThis.getS3SizeThreshold() != null && aThis.getS3SizeThreshold() > 0) {
                extendedClientConfig = extendedClientConfig.withPayloadSizeThreshold(aThis.getS3SizeThreshold());
            }
            if (aThis.getS3KeyPrefix() != null) {
                extendedClientConfig = extendedClientConfig.withS3KeyPrefix(aThis.getS3KeyPrefix());
            }
        }
        sqsExtClient = new AmazonSQSExtendedClient(sqsClient, extendedClientConfig);
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
         if (isLargeMessage(request.messageBody())) {
            return sqsExtClient.sendMessage(request);
        } else {
            return sqsClient.sendMessage(request);
        }
    }

    @Override
    public SendMessageResponse sendMessage(String queueURL, String messageBody) {
        if (isLargeMessage(messageBody)) {
            return sqsExtClient.sendMessage(SendMessageRequest.builder().queueUrl(queueURL).messageBody(messageBody).build());
        } else {
            return sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueURL).messageBody(messageBody).build());
        }
    }

    @Override
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest batch) {
        return sqsClient.sendMessageBatch(batch);
    }

    @Override
    public SendMessageBatchResponse sendMessageBatch(String queueURL, List<SendMessageBatchRequestEntry> entries) {
        return sqsClient.sendMessageBatch(SendMessageBatchRequest.builder().queueUrl(queueURL).entries(entries).build());
    }

    private boolean isLargeMessage(String messageBody) {
        return messageBody.length() > 256 * 1024; // 256KB
    }

    @Override
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        return sqsClient.receiveMessage(receiveMessageRequest);
    }

    @Override
    public ReceiveMessageResponse receiveMessage(Consumer<ReceiveMessageRequest.Builder> receiveMessageRequest) {
        return sqsClient.receiveMessage(receiveMessageRequest);
    }

    @Override
    public GetQueueAttributesResponse getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest) {
        return sqsClient.getQueueAttributes(getQueueAttributesRequest);
    }

    @Override
    public GetQueueAttributesResponse getQueueAttributes(Consumer<GetQueueAttributesRequest.Builder> getQueueAttributesRequest) {
        return sqsClient.getQueueAttributes(getQueueAttributesRequest);
    }

    @Override
    public SetQueueAttributesResponse setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) {
        return sqsClient.setQueueAttributes(setQueueAttributesRequest);
    }

    @Override
    public SetQueueAttributesResponse setQueueAttributes(Consumer<SetQueueAttributesRequest.Builder> setQueueAttributesRequest) {
        return sqsClient.setQueueAttributes(setQueueAttributesRequest);
    }

    @Override
    public CreateQueueResponse createQueue(CreateQueueRequest createQueueRequest) {
        return sqsClient.createQueue(createQueueRequest);
    }

    @Override
    public CreateQueueResponse createQueue(Consumer<CreateQueueRequest.Builder> createQueueRequest) {
        return sqsClient.createQueue(createQueueRequest);
    }

    @Override
    public DeleteMessageResponse deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        return sqsClient.deleteMessage(deleteMessageRequest);
    }

    @Override
    public DeleteMessageResponse deleteMessage(Consumer<DeleteMessageRequest.Builder> deleteMessageRequest) {
        return sqsClient.deleteMessage(deleteMessageRequest);
    }

    @Override
    public DeleteMessageBatchResponse deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {
        return sqsClient.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public DeleteMessageBatchResponse deleteMessageBatch(Consumer<DeleteMessageBatchRequest.Builder> deleteMessageBatchRequest) {
        return sqsClient.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public DeleteQueueResponse deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        return sqsClient.deleteQueue(deleteQueueRequest);
    }

    @Override
    public DeleteQueueResponse deleteQueue(Consumer<DeleteQueueRequest.Builder> deleteQueueRequest) {
        return sqsClient.deleteQueue(deleteQueueRequest);
    }

    @Override
    public GetQueueUrlResponse getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        return sqsClient.getQueueUrl(getQueueUrlRequest);
    }

    @Override
    public GetQueueUrlResponse getQueueUrl(Consumer<GetQueueUrlRequest.Builder> getQueueUrlRequest) {
        return sqsClient.getQueueUrl(getQueueUrlRequest);
    }

    @Override
    public ListQueueTagsResponse listQueueTags(ListQueueTagsRequest listQueueTagsRequest) {
        return sqsClient.listQueueTags(listQueueTagsRequest);
    }

    @Override
    public ListQueueTagsResponse listQueueTags(Consumer<ListQueueTagsRequest.Builder> listQueueTagsRequest) {
        return sqsClient.listQueueTags(listQueueTagsRequest);
    }

    @Override
    public ListQueuesResponse listQueues(ListQueuesRequest listQueuesRequest) {
        return sqsClient.listQueues(listQueuesRequest);
    }

    @Override
    public ListQueuesResponse listQueues() {
        return sqsClient.listQueues();
    }

    @Override
    public ListQueuesResponse listQueues(Consumer<ListQueuesRequest.Builder> listQueuesRequest) {
        return sqsClient.listQueues(listQueuesRequest);
    }

    @Override
    public PurgeQueueResponse purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        return sqsClient.purgeQueue(purgeQueueRequest);
    }

    @Override
    public TagQueueResponse tagQueue(TagQueueRequest tagQueueRequest) {
        return sqsClient.tagQueue(tagQueueRequest);
    }

    @Override
    public TagQueueResponse tagQueue(Consumer<TagQueueRequest.Builder> tagQueueRequest) {
        return sqsClient.tagQueue(tagQueueRequest);
    }

    @Override
    public UntagQueueResponse untagQueue(UntagQueueRequest untagQueueRequest) {
        return sqsClient.untagQueue(untagQueueRequest);
    }

    @Override
    public UntagQueueResponse untagQueue(Consumer<UntagQueueRequest.Builder> untagQueueRequest) {
        return sqsClient.untagQueue(untagQueueRequest);
    }

    @Override
    public AddPermissionResponse addPermission(AddPermissionRequest addPermissionRequest) {
        return sqsClient.addPermission(addPermissionRequest);
    }

    @Override
    public AddPermissionResponse addPermission(Consumer<AddPermissionRequest.Builder> addPermissionRequest) {
        return sqsClient.addPermission(addPermissionRequest);
    }

    @Override
    public CancelMessageMoveTaskResponse cancelMessageMoveTask(CancelMessageMoveTaskRequest cancelMessageMoveTaskRequest) {
        return sqsClient.cancelMessageMoveTask(cancelMessageMoveTaskRequest);
    }

    @Override
    public ChangeMessageVisibilityResponse changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {
        return sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    @Override
    public ChangeMessageVisibilityResponse changeMessageVisibility(Consumer<ChangeMessageVisibilityRequest.Builder> changeMessageVisibilityRequest) {
        return sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    @Override
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
        return sqsClient.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    @Override
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(Consumer<ChangeMessageVisibilityBatchRequest.Builder> changeMessageVisibilityBatchRequest) {
        return sqsClient.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    @Override
    public ListDeadLetterSourceQueuesResponse listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) {
        return sqsClient.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    @Override
    public ListMessageMoveTasksResponse listMessageMoveTasks(ListMessageMoveTasksRequest listMessageMoveTasksRequest) {
        return sqsClient.listMessageMoveTasks(listMessageMoveTasksRequest);
    }

    @Override
    public RemovePermissionResponse removePermission(RemovePermissionRequest removePermissionRequest) {
        return sqsClient.removePermission(removePermissionRequest);
    }

    @Override
    public RemovePermissionResponse removePermission(Consumer<RemovePermissionRequest.Builder> removePermissionRequest) {
        return sqsClient.removePermission(removePermissionRequest);
    }

    @Override
    public StartMessageMoveTaskResponse startMessageMoveTask(StartMessageMoveTaskRequest startMessageMoveTaskRequest) {
        return sqsClient.startMessageMoveTask(startMessageMoveTaskRequest);
    }

    @Override
    public void close() throws Exception {
        destroy();
    }

    private AwsCredentialsProvider getCredentials(AmazonSQSManagedConnectionFactory aThis) {
        AwsCredentialsProvider credentialsProvider;
        if (StringUtils.isNotBlank(aThis.getRoleArn())) {
            credentialsProvider = STSCredentialsProvider.create(aThis.getRoleArn(), aThis.getRoleSessionName(), Region.of(aThis.getRegion()));
        } else if (StringUtils.isNotBlank(aThis.getProfileName())) {
            credentialsProvider = ProfileCredentialsProvider.create(aThis.getProfileName());
        } else if (StringUtils.isNotBlank(aThis.getAwsAccessKeyId()) && StringUtils.isNotBlank(aThis.getAwsSecretKey())) {
            credentialsProvider = () -> AwsBasicCredentials.create(aThis.getAwsAccessKeyId(), aThis.getAwsSecretKey());
        } else {
            credentialsProvider = DefaultCredentialsProvider.create();
        }
        return credentialsProvider;
    }

}
