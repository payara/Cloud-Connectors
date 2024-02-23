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

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.sqs.model.*;
import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnection;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
class AmazonSQSConnectionImpl implements AmazonSQSConnection {

    private AmazonSQSManagedConnection underlyingConnection;

    AmazonSQSConnectionImpl(AmazonSQSManagedConnection realConn) {
        underlyingConnection = realConn;
    }

    @Override
    public SendMessageResult sendMessage(SendMessageRequest request) {
        return underlyingConnection.sendMessage(request);
    }

    @Override
    public SendMessageResult sendMessage(String queueURL, String messageBody) {
        return underlyingConnection.sendMessage(queueURL, messageBody);
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(String queueURL, List<SendMessageBatchRequestEntry> entries) {
        return underlyingConnection.sendMessageBatch(queueURL, entries);
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest batch) {
        return underlyingConnection.sendMessageBatch(batch);
    }

    @Override
    public void close() throws Exception {
        underlyingConnection.removeHandle(this);
    }

    void setRealConnection(AmazonSQSManagedConnection aThis) {
        this.underlyingConnection = aThis;
    }

    @Override
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        return underlyingConnection.receiveMessage(receiveMessageRequest);
    }

    @Override
    public ReceiveMessageResult receiveMessage(String queueUrl) {
        return underlyingConnection.receiveMessage(queueUrl);
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest) {
        return underlyingConnection.getQueueAttributes(getQueueAttributesRequest);
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(String queueUrl, List<String> attributeNames) {
        return underlyingConnection.getQueueAttributes(queueUrl, attributeNames);
    }

    @Override
    public SetQueueAttributesResult setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) {
        return underlyingConnection.setQueueAttributes(setQueueAttributesRequest);
    }

    @Override
    public SetQueueAttributesResult setQueueAttributes(String queueUrl, Map<String, String> attributes) {
        return underlyingConnection.setQueueAttributes(queueUrl, attributes);
    }

    @Override
    public CreateQueueResult createQueue(CreateQueueRequest createQueueRequest) {
        return underlyingConnection.createQueue(createQueueRequest);
    }

    @Override
    public CreateQueueResult createQueue(String queueName) {
        return underlyingConnection.createQueue(queueName);
    }

    @Override
    public DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        return underlyingConnection.deleteMessage(deleteMessageRequest);
    }

    @Override
    public DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle) {
        return underlyingConnection.deleteMessage(queueUrl, receiptHandle);
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {
        return underlyingConnection.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries) {
        return underlyingConnection.deleteMessageBatch(queueUrl, entries);
    }

    @Override
    public DeleteQueueResult deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        return underlyingConnection.deleteQueue(deleteQueueRequest);
    }

    @Override
    public DeleteQueueResult deleteQueue(String queueUrl) {
        return underlyingConnection.deleteQueue(queueUrl);
    }

    @Override
    public GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        return underlyingConnection.getQueueUrl(getQueueUrlRequest);
    }

    @Override
    public GetQueueUrlResult getQueueUrl(String queueName) {
        return underlyingConnection.getQueueUrl(queueName);
    }

    @Override
    public ListQueueTagsResult listQueueTags(ListQueueTagsRequest listQueueTagsRequest) {
        return underlyingConnection.listQueueTags(listQueueTagsRequest);
    }

    @Override
    public ListQueueTagsResult listQueueTags(String queueUrl) {
        return underlyingConnection.listQueueTags(queueUrl);
    }

    @Override
    public ListQueuesResult listQueues() {
        return underlyingConnection.listQueues();
    }

    @Override
    public ListQueuesResult listQueues(ListQueuesRequest listQueuesRequest) {
        return underlyingConnection.listQueues(listQueuesRequest);
    }

    @Override
    public ListQueuesResult listQueues(String queueNamePrefix) {
        return underlyingConnection.listQueues(queueNamePrefix);
    }

    @Override
    public PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        return underlyingConnection.purgeQueue(purgeQueueRequest);
    }

    @Override
    public TagQueueResult tagQueue(TagQueueRequest tagQueueRequest) {
        return underlyingConnection.tagQueue(tagQueueRequest);
    }

    @Override
    public TagQueueResult tagQueue(String queueUrl, Map<String, String> tags) {
        return underlyingConnection.tagQueue(queueUrl, tags);
    }

    @Override
    public UntagQueueResult untagQueue(UntagQueueRequest untagQueueRequest) {
        return underlyingConnection.untagQueue(untagQueueRequest);
    }

    @Override
    public UntagQueueResult untagQueue(String queueUrl, List<String> tagKeys) {
        return underlyingConnection.untagQueue(queueUrl, tagKeys);
    }

    // ... (Previous code)
    @Override
    public AddPermissionResult addPermission(AddPermissionRequest addPermissionRequest) {
        return underlyingConnection.addPermission(addPermissionRequest);
    }

    @Override
    public AddPermissionResult addPermission(String queueUrl, String label, List<String> awsAccountIds, List<String> actions) {
        return underlyingConnection.addPermission(queueUrl, label, awsAccountIds, actions);
    }

    @Override
    public CancelMessageMoveTaskResult cancelMessageMoveTask(CancelMessageMoveTaskRequest cancelMessageMoveTaskRequest) {
        return underlyingConnection.cancelMessageMoveTask(cancelMessageMoveTaskRequest);
    }

    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {
        return underlyingConnection.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl, String receiptHandle, Integer visibilityTimeout) {
        return underlyingConnection.changeMessageVisibility(queueUrl, receiptHandle, visibilityTimeout);
    }

    @Override
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
        return underlyingConnection.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    @Override
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(String queueUrl, List<ChangeMessageVisibilityBatchRequestEntry> entries) {
        return underlyingConnection.changeMessageVisibilityBatch(queueUrl, entries);
    }

    @Override
    public ListDeadLetterSourceQueuesResult listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) {
        return underlyingConnection.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    @Override
    public ListMessageMoveTasksResult listMessageMoveTasks(ListMessageMoveTasksRequest listMessageMoveTasksRequest) {
        return underlyingConnection.listMessageMoveTasks(listMessageMoveTasksRequest);
    }

    @Override
    public RemovePermissionResult removePermission(RemovePermissionRequest removePermissionRequest) {
        return underlyingConnection.removePermission(removePermissionRequest);
    }

    @Override
    public RemovePermissionResult removePermission(String queueUrl, String label) {
        return underlyingConnection.removePermission(queueUrl, label);
    }

    @Override
    public StartMessageMoveTaskResult startMessageMoveTask(StartMessageMoveTaskRequest startMessageMoveTaskRequest) {
        return underlyingConnection.startMessageMoveTask(startMessageMoveTaskRequest);
    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
        return underlyingConnection.getCachedResponseMetadata(amazonWebServiceRequest);
    }

}
