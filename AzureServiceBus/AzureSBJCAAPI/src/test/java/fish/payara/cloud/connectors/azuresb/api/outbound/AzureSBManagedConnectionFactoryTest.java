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
package fish.payara.cloud.connectors.azuresb.api.outbound;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AzureSBManagedConnectionFactoryTest {

    private AzureSBManagedConnectionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AzureSBManagedConnectionFactory();
        factory.setQueueName("queue1");
        factory.setSasKey("sas-key-value");
        factory.setSasKeyName("sas-key-name");
        factory.setNameSpace("test-namespace");
        factory.setTimeOut(1000);
    }

    @Test
    void testCreateConnectionFactoryWithManager() throws ResourceException {
        ConnectionManager cm = mock(ConnectionManager.class);
        Object cf = factory.createConnectionFactory(cm);
        assertNotNull(cf);
        assertTrue(cf instanceof AzureSBConnectionFactoryImpl);
    }

    @Test
    void testCreateConnectionFactoryWithoutManager() throws ResourceException {
        Object cf = factory.createConnectionFactory();
        assertNotNull(cf);
        assertTrue(cf instanceof AzureSBConnectionFactoryImpl);
    }

    @Test
    void testCreateManagedConnection() throws Exception {
        Subject subject = new Subject();
        ConnectionRequestInfo info = mock(ConnectionRequestInfo.class);
        ManagedConnection mc = factory.createManagedConnection(subject, info);
        assertNotNull(mc);
        assertTrue(mc instanceof AzureSBManagedConnection);
    }

}