/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.cloud.connectors.mqtt.api.inbound;

import fish.payara.cloud.connectors.mqtt.api.OnMQTTMessage;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.*;

class MQTTWorkTest {

    @Test
    void testRunInvokesAnnotatedMethodAndLifecycle() throws Exception {
        // Dummy endpoint class with annotated method
        class DummyEndpoint implements MessageEndpoint {
            boolean beforeCalled = false;
            boolean afterCalled = false;
            boolean released = false;
            boolean invoked = false;

            @OnMQTTMessage
            public void handle(String topic, MqttMessage msg) {
                invoked = true;
            }

            @Override
            public void beforeDelivery(Method method) {
                beforeCalled = true;
            }

            @Override
            public void afterDelivery() {
                afterCalled = true;
            }

            @Override
            public void release() {
                released = true;
            }
        }

        DummyEndpoint endpoint = spy(new DummyEndpoint());
        MessageEndpointFactory factory = mock(MessageEndpointFactory.class);
        when(factory.getEndpointClass()).thenAnswer(invocation -> (Class<?>) DummyEndpoint.class);
        when(factory.createEndpoint(null)).thenReturn(endpoint);

        MqttMessage mqttMessage = new MqttMessage("payload".getBytes());
        MQTTWork work = new MQTTWork("topic/test", mqttMessage, factory);

        work.run();

        verify(endpoint).beforeDelivery(any(Method.class));
        verify(endpoint).afterDelivery();
        verify(endpoint).release();
        assertTrue(endpoint.invoked);
        assertTrue(endpoint.beforeCalled);
        assertTrue(endpoint.afterCalled);
        assertTrue(endpoint.released);
    }

    @Test
    void testRunWithNoAnnotatedMethodDoesNothing() {
        class NoAnnotationEndpoint implements MessageEndpoint {
            @Override public void beforeDelivery(Method method) {}
            @Override public void afterDelivery() {}
            @Override public void release() {}
            public void notHandle(String topic, MqttMessage msg) {}
        }

        MessageEndpointFactory factory = mock(MessageEndpointFactory.class);
        when(factory.getEndpointClass()).thenAnswer(invocation -> (Class<?>) NoAnnotationEndpoint.class);

        MQTTWork work = new MQTTWork("topic/test", new MqttMessage("payload".getBytes()), factory);

        work.run();
    }

    @Test
    void testReleaseCallsReleaseEndpoint() throws Exception {
        class DummyEndpoint implements MessageEndpoint {
            boolean released = false;
            @OnMQTTMessage
            public void handle(String topic, MqttMessage msg) {}
            @Override public void beforeDelivery(Method method) {}
            @Override public void afterDelivery() {}
            @Override public void release() { released = true; }
        }

        DummyEndpoint endpoint = spy(new DummyEndpoint());
        MessageEndpointFactory factory = mock(MessageEndpointFactory.class);
        when(factory.getEndpointClass()).thenAnswer(invocation -> (Class<?>) DummyEndpoint.class);
        when(factory.createEndpoint(null)).thenReturn(endpoint);

        MQTTWork work = new MQTTWork("topic/test", new MqttMessage("payload".getBytes()), factory);
        work.run();
        work.release();

        verify(endpoint, atLeastOnce()).release();
    }
}