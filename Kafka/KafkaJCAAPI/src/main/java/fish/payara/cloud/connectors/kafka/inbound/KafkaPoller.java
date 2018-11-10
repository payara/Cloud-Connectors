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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final Logger LOGGER = Logger.getLogger(KafkaPoller.class.getName());

    private KafkaConsumer consumer;
    KafkaActivationSpec kSpec;
    BootstrapContext context;
    private MessageEndpointFactory endpointFactory;

    private final List<Method> onRecordMethods = new ArrayList<>();
    private final List<Method> onRecordsMethods = new ArrayList<>();

    KafkaPoller(KafkaActivationSpec kSpec, BootstrapContext context, MessageEndpointFactory endpointFactory) {
        this.kSpec = kSpec;
        this.context = context;
        this.endpointFactory = endpointFactory;
        consumer = new KafkaConsumer(kSpec.getConsumerProperties());
        consumer.subscribe(Arrays.asList(kSpec.getTopics().split(",")));

        Class<?> mdbClass = endpointFactory.getEndpointClass();
        for (Method m : mdbClass.getMethods()) {
            if (m.isAnnotationPresent(OnRecord.class)) {
                if (m.getParameterCount() == 1 && ConsumerRecord.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    onRecordMethods.add(m);
                } else {
                    LOGGER.log(Level.WARNING, "@{0} annotated MDBs must have only one parameter of type {1}. {2}#{3} endpoint will be ignored.", new Object[]{
                        OnRecord.class.getSimpleName(), ConsumerRecord.class.getSimpleName(), mdbClass.getName(), m.getName()});
                }
            }

            if (m.isAnnotationPresent(OnRecords.class)) {
                if (m.getParameterCount() == 1 && ConsumerRecords.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    onRecordsMethods.add(m);
                } else {
                    LOGGER.log(Level.WARNING, "@{0} annotated MDBs must have only one parameter of type {1}. {2}#{3} endpoint will be ignored.", new Object[]{
                        OnRecords.class.getSimpleName(), ConsumerRecords.class.getSimpleName(), mdbClass.getName(), m.getName()});
                }
            }
        }
    }

    @Override
    public void run() {
        ConsumerRecords<Object, Object> records = consumer.poll(Duration.of(kSpec.getPollInterval(), ChronoUnit.MILLIS));

        // if we got noting just return
        if (records.isEmpty()) {
            return;
        }

        // search for methods with OnRecords annotation. This takes precedence.
        for (Method m : onRecordsMethods) {
            try {
                // method receives a ConsumerRecord
                OnRecords recordsAnnt = m.getAnnotation(OnRecords.class);
                context.getWorkManager().scheduleWork(new KafkaWork(endpointFactory, m, records));
                if (!recordsAnnt.matchOtherMethods()) {
                    return;
                }
            } catch (WorkException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        // match methods with OnRecord annotation
        for (ConsumerRecord<Object, Object> record : records) {
            for (Method m : onRecordMethods) {
                try {
                    // method receives a ConsumerRecord
                    OnRecord recordAnnt = m.getAnnotation(OnRecord.class);
                    String topics[] = recordAnnt.topics();
                    if ((topics.length == 0) || Arrays.binarySearch(recordAnnt.topics(), record.topic()) >= 0) {
                        context.getWorkManager().scheduleWork(new KafkaWork(endpointFactory, m, record));
                        if (!recordAnnt.matchOtherMethods()) {
                            break;
                        }
                    }
                } catch (WorkException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
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
