/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkException;
import jakarta.resource.spi.work.WorkManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class KafkaAsynchWorker implements KafkaWorker {

    private static final Logger LOGGER = Logger.getLogger(KafkaAsynchWorker.class.getName());
    private final EndpointKey key;
    private final KafkaConsumer consumer;
    private final WorkManager wm;
    private AtomicBoolean stopped = new AtomicBoolean();

    private final List<Method> onRecordMethods = new ArrayList<>();
    private final List<Method> onRecordsMethods = new ArrayList<>();

    private final long startTime = new Date().getTime();
    private AtomicBoolean started = new AtomicBoolean();


    public KafkaAsynchWorker(EndpointKey key, WorkManager wm) {
        // new work for the specified key
        this.key = key;
        this.wm = wm;
        stopped.set(false);

        // work out what methods we have
        Class<?> mdbClass = key.getMef().getEndpointClass();
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

        consumer = new KafkaConsumer(key.getSpec().getConsumerProperties());
        consumer.subscribe(Arrays.asList(key.getSpec().getTopics().split(",")));
    }

    @Override
    public void stop() {
        stopped.set(true);
    }

    @Override
    public boolean isStopped() {
        return stopped.get();
    }

    private void checkStart(){
        long now = System.currentTimeMillis();
        if (now >key.getSpec().getInitialPollDelay()+startTime) {
            started.set(true);
        }else {
            try {
                Thread.sleep(key.getSpec().getInitialPollDelay() - (now - startTime));
                started.set(true);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupt during sleep while waiting for initial poll delay");
            }
        }
    }
    
    @Override
    public void release() {
        stop();
    }

    @Override
    public void run() {
        if (!started.get()) {
            checkStart();
        }else{
            ConsumerRecords<Object, Object> records = consumer.poll(Duration.of(key.getSpec().getPollInterval(), ChronoUnit.MILLIS));
            if (!records.isEmpty()) {
                try {
                    // ok we got something
                    // schedule on the Work Manager Again
                    wm.scheduleWork(new RecordsProcessor(records));
                } catch (WorkException ex) {
                    Logger.getLogger(KafkaAsynchWorker.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (key.getSpec().getCommitEachPoll()) {
                    consumer.commitSync();
                }
            }

            // consume records
            if (stopped.get()) {
                consumer.close();
            }
        }
    }

    private class RecordsProcessor implements Work {

        private ConsumerRecords<Object, Object> records;
        private MessageEndpoint endpoint;

        RecordsProcessor(ConsumerRecords<Object, Object> records) {
            this.records = records;
        }

        @Override
        public void release() {
        }

        @Override
        public void run() {
            try {
                endpoint = key.getMef().createEndpoint(null);

                // search for methods with OnRecords annotation. This takes precedence.
                for (Method m : onRecordsMethods) {
                    // method receives a ConsumerRecord
                    OnRecords recordsAnnt = m.getAnnotation(OnRecords.class);
                    try {
                        deliverRecords(endpoint, m, records);
                    } catch (UnavailableException ex) {
                        Logger.getLogger(KafkaSynchWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (!recordsAnnt.matchOtherMethods()) {
                        return;
                    }
                }

                // match methods with OnRecord annotation
                for (ConsumerRecord<Object, Object> record : records) {
                    for (Method m : onRecordMethods) {
                        // method receives a ConsumerRecord
                        OnRecord recordAnnt = m.getAnnotation(OnRecord.class);
                        String topics[] = recordAnnt.topics();
                        if ((topics.length == 0) || Arrays.binarySearch(recordAnnt.topics(), record.topic()) >= 0) {
                            try {
                                deliverRecord(endpoint, m, record);
                            } catch (UnavailableException ex) {
                                Logger.getLogger(KafkaSynchWorker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            if (!recordAnnt.matchOtherMethods()) {
                                break;
                            }
                        }
                    }
                }
            } catch (UnavailableException ex) {
                Logger.getLogger(KafkaAsynchWorker.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                endpoint.release();
            }
        }

        private void deliverRecords(MessageEndpoint endpoint, Method m, ConsumerRecords<Object, Object> records) throws UnavailableException {
            try {
                endpoint.beforeDelivery(m);
                m.invoke(endpoint, records);
                endpoint.afterDelivery();
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ResourceException ex) {
                Logger.getLogger(KafkaResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void deliverRecord(MessageEndpoint endpoint, Method m, ConsumerRecord<Object, Object> record) throws UnavailableException {
            try {
                endpoint.beforeDelivery(m);
                m.invoke(endpoint, record);
                endpoint.afterDelivery();
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ResourceException ex) {
                Logger.getLogger(KafkaResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

}
