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
package fish.payara.cloud.connectors.amazonsqs.api;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
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
import java.util.List;
import java.util.Map;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public interface AmazonSQSConnection extends AutoCloseable {

    SendMessageResult sendMessage(SendMessageRequest request);

    SendMessageResult sendMessage(String queueURL, String messageBody);

    SendMessageBatchResult sendMessageBatch(String queueURL, List<SendMessageBatchRequestEntry> entries);

    SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest batch);

    ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest);

    ReceiveMessageResult receiveMessage(String queueUrl);

    GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest);

    GetQueueAttributesResult getQueueAttributes(String queueUrl, List<String> attributeNames);

    SetQueueAttributesResult setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest);

    SetQueueAttributesResult setQueueAttributes(String queueUrl, Map<String, String> attributes);

    CreateQueueResult createQueue(CreateQueueRequest createQueueRequest);

    CreateQueueResult createQueue(String queueName);

    DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest);

    DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle);

    DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest);

    DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries);

    DeleteQueueResult deleteQueue(DeleteQueueRequest deleteQueueRequest);

    DeleteQueueResult deleteQueue(String queueUrl);

    GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest);

    GetQueueUrlResult getQueueUrl(String queueName);

    ListQueueTagsResult listQueueTags(ListQueueTagsRequest listQueueTagsRequest);

    ListQueueTagsResult listQueueTags(String queueUrl);

    ListQueuesResult listQueues();

    ListQueuesResult listQueues(ListQueuesRequest listQueuesRequest);

    ListQueuesResult listQueues(String queueNamePrefix);

    PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest);

    TagQueueResult tagQueue(TagQueueRequest tagQueueRequest);

    TagQueueResult tagQueue(String queueUrl, Map<String, String> tags);

    UntagQueueResult untagQueue(UntagQueueRequest untagQueueRequest);

    UntagQueueResult untagQueue(String queueUrl, List<String> tagKeys);

    AddPermissionResult addPermission(AddPermissionRequest addPermissionRequest);

    AddPermissionResult addPermission(String queueUrl, String label, List<String> awsAccountIds, List<String> actions);

    RemovePermissionResult removePermission(RemovePermissionRequest removePermissionRequest);

    RemovePermissionResult removePermission(String queueUrl, String label);

    ListMessageMoveTasksResult listMessageMoveTasks(ListMessageMoveTasksRequest listMessageMoveTasksRequest);

    StartMessageMoveTaskResult startMessageMoveTask(StartMessageMoveTaskRequest startMessageMoveTaskRequest);

    CancelMessageMoveTaskResult cancelMessageMoveTask(CancelMessageMoveTaskRequest cancelMessageMoveTaskRequest);

    ChangeMessageVisibilityResult changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest);

    ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl, String receiptHandle, Integer visibilityTimeout);

    ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest);

    ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(String queueUrl, List<ChangeMessageVisibilityBatchRequestEntry> entries);

    ListDeadLetterSourceQueuesResult listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest);

    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest);

}
