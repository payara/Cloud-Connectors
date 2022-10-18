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
package fish.payara.cloud.connectors.amazonsqs.api.inbound;

import fish.payara.cloud.connectors.amazonsqs.api.OnSQSMessage;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.WorkException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * @author Steve Millidge (Payara Foundation)
 */
class SQSPoller extends TimerTask {

    AmazonSQSActivationSpec spec;
    BootstrapContext ctx;
    MessageEndpointFactory factory;
    SqsClient client;

    SQSPoller(AmazonSQSActivationSpec sqsSpec, BootstrapContext context, MessageEndpointFactory endpointFactory) {
        spec = sqsSpec;
        ctx = context;
        factory = endpointFactory;
        client = SqsClient.builder().region(Region.of(spec.getRegion()))
                .credentialsProvider(spec).build();
    }

    @Override
    public void run() {
        try {
            ReceiveMessageRequest rmr = ReceiveMessageRequest.builder()
                    .queueUrl(spec.getQueueURL())
                    .maxNumberOfMessages(spec.getMaxMessages()).visibilityTimeout(spec.getVisibilityTimeout())
                    .waitTimeSeconds(spec.getPollInterval() / 1000)
                    .attributeNames(Arrays.stream(spec.getAttributeNames().split(","))
                            .map(s -> QueueAttributeName.fromValue(s)).collect(Collectors.toList()))
                    .messageAttributeNames(Arrays.asList(spec.getMessageAttributeNames().split(","))).build();
            List<Message> messages = client.receiveMessage(rmr).messages();
            if (!messages.isEmpty()) {
                Class<?> mdbClass = factory.getEndpointClass();
                for (Message message : messages) {
                    for (Method m : mdbClass.getMethods()) {
                        if (m.isAnnotationPresent(OnSQSMessage.class) && m.getParameterCount() == 1
                                && m.getParameterTypes()[0].equals(Message.class)) {
                            try {
                                ctx.getWorkManager()
                                        .scheduleWork(new SQSWork(client, factory, m, message, spec.getQueueURL()));
                            } catch (WorkException ex) {
                                Logger.getLogger(AmazonSQSResourceAdapter.class.getName())
                                        .log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                }
            }
        } catch (IllegalStateException ise) {
            // Fix #29 ensure Illegal State Exception doesn't blow up the timer
            Logger.getLogger(AmazonSQSResourceAdapter.class.getName())
                    .log(Level.WARNING, "Poller caught an Illegal State Exception", ise);
        } catch (Exception e) {
            Logger.getLogger(AmazonSQSResourceAdapter.class.getName())
                    .log(Level.WARNING, "Poller caught an Unexpected Exception", e);
        }
    }

}
