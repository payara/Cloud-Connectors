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

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MQTTResourceAdapterTest {

    private MQTTResourceAdapter adapter;
    private BootstrapContext bootstrapContext;
    private MessageEndpointFactory endpointFactory;
    private ActivationSpec activationSpec;

    @BeforeEach
    void setUp() {
        adapter = new TestMQTTResourceAdapter();
        bootstrapContext = mock(BootstrapContext.class);
        endpointFactory = mock(MessageEndpointFactory.class);
        activationSpec = mock(ActivationSpec.class);
    }

    @Test
    void testStartSetsContext() throws Exception {
        adapter.start(bootstrapContext);
        var contextField = MQTTResourceAdapter.class.getDeclaredField("context");
        contextField.setAccessible(true);
        assertEquals(bootstrapContext, contextField.get(adapter));
    }

    @Test
    void testGetXAResourcesReturnsNull() throws Exception {
        XAResource[] result = adapter.getXAResources(new ActivationSpec[0]);
        assertNull(result);
    }

    @Test
    void testEqualsAndHashCode() {
        MQTTResourceAdapter adapter2 = new MQTTResourceAdapter();
        assertEquals(adapter, adapter);
        assertNotEquals(adapter, adapter2); // default equals uses reference
        assertEquals(adapter.hashCode(), adapter.hashCode());
    }

    static class TestMQTTResourceAdapter extends MQTTResourceAdapter {
        protected MQTTSubscriber createSubscriber(MessageEndpointFactory endpointFactory, ActivationSpec spec, jakarta.resource.spi.work.WorkManager workManager) {
            try {
                MQTTSubscriber subscriber = spy(new MQTTSubscriber(endpointFactory, spec, workManager));
                var clientField = MQTTSubscriber.class.getDeclaredField("client");
                clientField.setAccessible(true);
                clientField.set(subscriber, mock(org.eclipse.paho.client.mqttv3.MqttClient.class));
                doNothing().when(subscriber).subscribe();
                doNothing().when(subscriber).close();
                return subscriber;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}