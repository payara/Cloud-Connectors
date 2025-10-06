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

import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.ResourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MQTTActivationSpecTest {

    @Test
    void testDefaultValues() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        assertEquals("tcp://localhost:1883", spec.getServerURIs());
        assertFalse(spec.getCleanSession());
        assertTrue(spec.getAutomaticReconnect());
        assertFalse(spec.getFilePersistance());
        assertEquals(".", spec.getPersistenceDirectory());
        assertEquals(30, spec.getConnectionTimeout());
        assertEquals(10, spec.getMaxInflight());
        assertEquals(60, spec.getKeepAliveInterval());
        assertNull(spec.getUserName());
        assertNull(spec.getPassword());
        assertNull(spec.getTopicFilter());
        assertEquals(0, spec.getQos());
        assertNull(spec.getClientId());
    }

    @Test
    void testSettersAndGetters() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        spec.setServerURIs("tcp://test:1883");
        spec.setCleanSession(true);
        spec.setAutomaticReconnect(false);
        spec.setFilePersistance(true);
        spec.setPersistenceDirectory("/tmp");
        spec.setConnectionTimeout(15);
        spec.setMaxInflight(20);
        spec.setKeepAliveInterval(120);
        spec.setUserName("user");
        spec.setPassword("pass");
        spec.setTopicFilter("topic/test");
        spec.setQos(2);
        spec.setClientId("client123");

        assertEquals("tcp://test:1883", spec.getServerURIs());
        assertTrue(spec.getCleanSession());
        assertFalse(spec.getAutomaticReconnect());
        assertTrue(spec.getFilePersistance());
        assertEquals("/tmp", spec.getPersistenceDirectory());
        assertEquals(15, spec.getConnectionTimeout());
        assertEquals(20, spec.getMaxInflight());
        assertEquals(120, spec.getKeepAliveInterval());
        assertEquals("user", spec.getUserName());
        assertEquals("pass", spec.getPassword());
        assertEquals("topic/test", spec.getTopicFilter());
        assertEquals(2, spec.getQos());
        assertEquals("client123", spec.getClientId());
    }

    @Test
    void testValidateThrowsIfTopicFilterNull() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        spec.setTopicFilter(null);
        assertThrows(InvalidPropertyException.class, spec::validate);
    }

    @Test
    void testValidateSucceedsIfTopicFilterSet() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        spec.setTopicFilter("topic/test");
        assertDoesNotThrow(spec::validate);
    }

    @Test
    void testResourceAdapterSetAndGet() throws ResourceException {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        ResourceAdapter adapter = mock(ResourceAdapter.class);
        spec.setResourceAdapter(adapter);
        assertEquals(adapter, spec.getResourceAdapter());
    }
}