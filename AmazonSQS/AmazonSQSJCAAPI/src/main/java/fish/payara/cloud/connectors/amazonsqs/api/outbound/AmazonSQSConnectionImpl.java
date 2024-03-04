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

import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnection;
import java.util.List;
import java.util.function.Consumer;
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
    public SendMessageResponse sendMessage(SendMessageRequest request) {
        return underlyingConnection.sendMessage(request);
    }

    @Override
    public SendMessageResponse sendMessage(String queueURL, String messageBody) {
        return underlyingConnection.sendMessage(queueURL, messageBody);
    }

    @Override
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest batch) {
        return underlyingConnection.sendMessageBatch(batch);
    }

    @Override
    public SendMessageBatchResponse sendMessageBatch(String queueURL, List<SendMessageBatchRequestEntry> entries) {
        return underlyingConnection.sendMessageBatch(queueURL, entries);
    }

    @Override
    public void close() throws Exception {
        underlyingConnection.removeHandle(this);
    }

    void setRealConnection(AmazonSQSManagedConnection aThis) {
        this.underlyingConnection = aThis;
    }

    @Override
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        return underlyingConnection.receiveMessage(receiveMessageRequest);
    }

    @Override
    public ReceiveMessageResponse receiveMessage(Consumer<ReceiveMessageRequest.Builder> receiveMessageRequest) {
        return underlyingConnection.receiveMessage(receiveMessageRequest);
    }

    @Override
    public GetQueueAttributesResponse getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest) {
        return underlyingConnection.getQueueAttributes(getQueueAttributesRequest);
    }

    @Override
    public GetQueueAttributesResponse getQueueAttributes(Consumer<GetQueueAttributesRequest.Builder> getQueueAttributesRequest) {
        return underlyingConnection.getQueueAttributes(getQueueAttributesRequest);
    }

    @Override
    public SetQueueAttributesResponse setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) {
        return underlyingConnection.setQueueAttributes(setQueueAttributesRequest);
    }

    @Override
    public SetQueueAttributesResponse setQueueAttributes(Consumer<SetQueueAttributesRequest.Builder> setQueueAttributesRequest) {
        return underlyingConnection.setQueueAttributes(setQueueAttributesRequest);
    }

    @Override
    public CreateQueueResponse createQueue(CreateQueueRequest createQueueRequest) {
        return underlyingConnection.createQueue(createQueueRequest);
    }

    @Override
    public CreateQueueResponse createQueue(Consumer<CreateQueueRequest.Builder> createQueueRequest) {
        return underlyingConnection.createQueue(createQueueRequest);
    }

    @Override
    public DeleteMessageResponse deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        return underlyingConnection.deleteMessage(deleteMessageRequest);
    }

    @Override
    public DeleteMessageResponse deleteMessage(Consumer<DeleteMessageRequest.Builder> deleteMessageRequest) {
        return underlyingConnection.deleteMessage(deleteMessageRequest);
    }

    @Override
    public DeleteMessageBatchResponse deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {
        return underlyingConnection.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public DeleteMessageBatchResponse deleteMessageBatch(Consumer<DeleteMessageBatchRequest.Builder> deleteMessageBatchRequest) {
        return underlyingConnection.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public DeleteQueueResponse deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        return underlyingConnection.deleteQueue(deleteQueueRequest);
    }

    @Override
    public DeleteQueueResponse deleteQueue(Consumer<DeleteQueueRequest.Builder> deleteQueueRequest) {
        return underlyingConnection.deleteQueue(deleteQueueRequest);
    }

    @Override
    public GetQueueUrlResponse getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        return underlyingConnection.getQueueUrl(getQueueUrlRequest);
    }

    @Override
    public GetQueueUrlResponse getQueueUrl(Consumer<GetQueueUrlRequest.Builder> getQueueUrlRequest) {
        return underlyingConnection.getQueueUrl(getQueueUrlRequest);
    }

    @Override
    public ListQueueTagsResponse listQueueTags(ListQueueTagsRequest listQueueTagsRequest) {
        return underlyingConnection.listQueueTags(listQueueTagsRequest);
    }

    @Override
    public ListQueueTagsResponse listQueueTags(Consumer<ListQueueTagsRequest.Builder> listQueueTagsRequest) {
        return underlyingConnection.listQueueTags(listQueueTagsRequest);
    }

    @Override
    public ListQueuesResponse listQueues() {
        return underlyingConnection.listQueues();
    }

    @Override
    public ListQueuesResponse listQueues(ListQueuesRequest listQueuesRequest) {
        return underlyingConnection.listQueues(listQueuesRequest);
    }

    @Override
    public ListQueuesResponse listQueues(Consumer<ListQueuesRequest.Builder> listQueuesRequest) {
        return underlyingConnection.listQueues(listQueuesRequest);
    }

    @Override
    public PurgeQueueResponse purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        return underlyingConnection.purgeQueue(purgeQueueRequest);
    }

    @Override
    public TagQueueResponse tagQueue(TagQueueRequest tagQueueRequest) {
        return underlyingConnection.tagQueue(tagQueueRequest);
    }

    @Override
    public TagQueueResponse tagQueue(Consumer<TagQueueRequest.Builder> tagQueueRequest) {
        return underlyingConnection.tagQueue(tagQueueRequest);
    }

    @Override
    public UntagQueueResponse untagQueue(UntagQueueRequest untagQueueRequest) {
        return underlyingConnection.untagQueue(untagQueueRequest);
    }

    @Override
    public UntagQueueResponse untagQueue(Consumer<UntagQueueRequest.Builder> untagQueueRequest) {
        return underlyingConnection.untagQueue(untagQueueRequest);
    }

    @Override
    public AddPermissionResponse addPermission(AddPermissionRequest addPermissionRequest) {
        return underlyingConnection.addPermission(addPermissionRequest);
    }

    @Override
    public AddPermissionResponse addPermission(Consumer<AddPermissionRequest.Builder> addPermissionRequest) {
        return underlyingConnection.addPermission(addPermissionRequest);
    }

    @Override
    public CancelMessageMoveTaskResponse cancelMessageMoveTask(CancelMessageMoveTaskRequest cancelMessageMoveTaskRequest) {
        return underlyingConnection.cancelMessageMoveTask(cancelMessageMoveTaskRequest);
    }

    @Override
    public ChangeMessageVisibilityResponse changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {
        return underlyingConnection.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    @Override
    public ChangeMessageVisibilityResponse changeMessageVisibility(Consumer<ChangeMessageVisibilityRequest.Builder> changeMessageVisibilityRequest) {
        return underlyingConnection.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    @Override
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
        return underlyingConnection.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    @Override
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(Consumer<ChangeMessageVisibilityBatchRequest.Builder> changeMessageVisibilityBatchRequest) {
        return underlyingConnection.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    @Override
    public ListDeadLetterSourceQueuesResponse listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) {
        return underlyingConnection.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    @Override
    public ListMessageMoveTasksResponse listMessageMoveTasks(ListMessageMoveTasksRequest listMessageMoveTasksRequest) {
        return underlyingConnection.listMessageMoveTasks(listMessageMoveTasksRequest);
    }

    @Override
    public RemovePermissionResponse removePermission(RemovePermissionRequest removePermissionRequest) {
        return underlyingConnection.removePermission(removePermissionRequest);
    }

    @Override
    public RemovePermissionResponse removePermission(Consumer<RemovePermissionRequest.Builder> removePermissionRequest) {
        return underlyingConnection.removePermission(removePermissionRequest);
    }

    @Override
    public StartMessageMoveTaskResponse startMessageMoveTask(StartMessageMoveTaskRequest startMessageMoveTaskRequest) {
        return underlyingConnection.startMessageMoveTask(startMessageMoveTaskRequest);
    }

}
