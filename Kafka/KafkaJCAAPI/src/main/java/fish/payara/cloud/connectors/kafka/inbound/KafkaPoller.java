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
package fish.payara.cloud.connectors.kafka.inbound;

import fish.payara.cloud.connectors.kafka.api.OnRecord;
import fish.payara.cloud.connectors.kafka.api.OnRecords;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
class KafkaPoller extends TimerTask {

    private KafkaConsumer consumer;
    KafkaActivationSpec kSpec;
    BootstrapContext context;
    private MessageEndpointFactory endpointFactory;

    KafkaPoller(KafkaActivationSpec kSpec, BootstrapContext context, MessageEndpointFactory endpointFactory) {
        this.kSpec = kSpec;
        this.context = context;
        this.endpointFactory = endpointFactory;
        consumer = new KafkaConsumer(kSpec.getConsumerProperties());
        consumer.subscribe(Arrays.asList(kSpec.getTopics().split(",")));
    }

    @Override
    public void run() {
        ConsumerRecords<Object, Object> records = consumer.poll(kSpec.getPollInterval());
        
        // if we got noting just return
        if (records.isEmpty()) {
            return;
        }
        
        Class<?> mdbClass = endpointFactory.getEndpointClass();
        
        // search for methods with OnRecords annotation. This takes precedent.
        for (Method m : mdbClass.getMethods()) {
            if (m.isAnnotationPresent(OnRecords.class) && m.getParameterCount() == 1) {
                try {
                    // method receives a ConsumerRecord
                    OnRecords recordsAttn;
                    recordsAttn = m.getAnnotation(OnRecords.class);                 
                    context.getWorkManager().scheduleWork(new KafkaWork(endpointFactory, m, records));
                    if (!recordsAttn.matchOtherMethods()) {
                        return;
                    }
                } catch (WorkException ex) {
                    Logger.getLogger(KafkaResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }   
        
        // match methods with OnRecord annotation
        for (ConsumerRecord<Object, Object> record : records) {
            for (Method m : mdbClass.getMethods()) {
                if (m.isAnnotationPresent(OnRecord.class) && m.getParameterCount() == 1) {
                    try {
                        // method receives a ConsumerRecord
                        OnRecord recordAttn;
                        recordAttn = m.getAnnotation(OnRecord.class);
                        String topics[] = recordAttn.topics();
                        if ((topics.length == 0) || Arrays.binarySearch(recordAttn.topics(), record.topic()) >=0 ){
                            context.getWorkManager().scheduleWork(new KafkaWork(endpointFactory, m, record));
                            if (!recordAttn.matchOtherMethods()) {
                                break;
                            }
                        }
                    } catch (WorkException ex) {
                        Logger.getLogger(KafkaResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    
    void stop() {
        if (consumer != null) {
            consumer.close();
        }
    }
}
