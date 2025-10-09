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
package fish.payara.cloud.connectors.azuresb.api.inbound;

import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AzureSBActivationSpecTest {

    @Test
    void testSettersAndGetters() {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        spec.setQueueName("queue1");
        spec.setSasKey("sas-key-value");
        spec.setSasKeyName("sas-key-name");
        spec.setNameSpace("test-namespace");
        assertEquals("queue1", spec.getQueueName());
        assertEquals("sas-key-value", spec.getSasKey());
        assertEquals("sas-key-name", spec.getSasKeyName());
        assertEquals("test-namespace", spec.getNameSpace());
    }

    @Test
    void testValidateThrowsIfQueueNameNull() {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        spec.setSasKey("sas-key-value");
        spec.setSasKeyName("sas-key-name");
        spec.setNameSpace("test-namespace");
        assertThrows(InvalidPropertyException.class, spec::validate);
    }

    @Test
    void testValidateThrowsIfSasKeyNull() {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        spec.setQueueName("queue1");
        spec.setSasKeyName("sas-key-name");
        spec.setNameSpace("test-namespace");
        assertThrows(InvalidPropertyException.class, spec::validate);
    }

    @Test
    void testValidateThrowsIfSasKeyNameNull() {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        spec.setQueueName("queue1");
        spec.setSasKey("sas-key-value");
        spec.setNameSpace("test-namespace");
        assertThrows(InvalidPropertyException.class, spec::validate);
    }

    @Test
    void testValidateThrowsIfNamespaceNull() {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        spec.setQueueName("queue1");
        spec.setSasKey("sas-key-value");
        spec.setSasKeyName("sas-key-name");
        assertThrows(InvalidPropertyException.class, spec::validate);
    }

    @Test
    void testValidateSucceedsIfAllSet() {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        spec.setQueueName("queue1");
        spec.setSasKey("sas-key-value");
        spec.setSasKeyName("sas-key-name");
        spec.setNameSpace("test-namespace");
        assertDoesNotThrow(spec::validate);
    }

    @Test
    void testResourceAdapterSetAndGet() throws Exception {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        ResourceAdapter adapter = mock(ResourceAdapter.class);
        spec.setResourceAdapter(adapter);
        assertEquals(adapter, spec.getResourceAdapter());
    }
}