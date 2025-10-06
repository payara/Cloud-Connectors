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

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import java.util.Timer;
import javax.transaction.xa.XAResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

class AzureSBResourceAdapterTest {

    private AzureSBResourceAdapter adapter;
    private BootstrapContext bootstrapContext;
    private MessageEndpointFactory endpointFactory;
    private ActivationSpec activationSpec;
    private AzureSBListener listener;
    private Timer poller;

    @BeforeEach
    void setUp() throws ResourceAdapterInternalException, ResourceException {
        adapter = new AzureSBResourceAdapter();
        bootstrapContext = mock(BootstrapContext.class);
        endpointFactory = mock(MessageEndpointFactory.class);
        activationSpec = mock(ActivationSpec.class);
        listener = mock(AzureSBListener.class);
        poller = mock(Timer.class);

        when(endpointFactory.getEndpointClass()).thenAnswer(invocation -> Object.class);
        when(bootstrapContext.getWorkManager()).thenReturn(mock(jakarta.resource.spi.work.WorkManager.class));
        adapter.start(bootstrapContext);
    }

    @Test
    void testStartSetsContext() throws Exception {
        var contextField = AzureSBResourceAdapter.class.getDeclaredField("context");
        contextField.setAccessible(true);
        assertEquals(bootstrapContext, contextField.get(adapter));
    }

    @Disabled("Cannot mock poller.cancel() in AzureSBResourceAdapter")
    @Test
    void testStopDoesNotThrow() {
        assertDoesNotThrow(() -> adapter.stop());
    }

    @Disabled("Cannot mock sbListener.subscribe() in AzureSBResourceAdapter")
    @Test
    void testEndpointActivationAndDeactivation() {
        AzureSBActivationSpec spec = new AzureSBActivationSpec();
        spec.setQueueName("queue1");
        spec.setSasKey("sas-key-value");
        spec.setSasKeyName("sas-key-name");
        spec.setNameSpace("test-namespace");
        assertDoesNotThrow(() -> adapter.endpointActivation(endpointFactory, spec));
        assertDoesNotThrow(() -> adapter.endpointDeactivation(endpointFactory, spec));
    }

    @Test
    void testGetXAResourcesReturnsNull() throws Exception {
        XAResource[] result = adapter.getXAResources(new ActivationSpec[0]);
        assertNull(result);
    }
}