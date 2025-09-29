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
package fish.payara.cloud.connectors.amazonsqs.api.outbound;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmazonSQSManagedConnectionFactoryTest {

    private AmazonSQSManagedConnectionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AmazonSQSManagedConnectionFactory();
    }

    @Test
    void testSettersAndGetters() {
        factory.setAwsSecretKey("secret");
        factory.setAwsAccessKeyId("access");
        factory.setRegion("us-east-1");
        factory.setProfileName("profile");
        factory.setRoleArn("arn:aws:iam::123456789012:role/test");
        factory.setRoleSessionName("session");
        factory.setS3BucketName("bucket");
        factory.setS3SizeThreshold(100);
        factory.setS3KeyPrefix("prefix/");
        factory.setS3FetchMessage(false);

        assertEquals("secret", factory.getAwsSecretKey());
        assertEquals("access", factory.getAwsAccessKeyId());
        assertEquals("us-east-1", factory.getRegion());
        assertEquals("profile", factory.getProfileName());
        assertEquals("arn:aws:iam::123456789012:role/test", factory.getRoleArn());
        assertEquals("session", factory.getRoleSessionName());
        assertEquals("bucket", factory.getS3BucketName());
        assertEquals(100, factory.getS3SizeThreshold());
        assertEquals("prefix/", factory.getS3KeyPrefix());
        assertFalse(factory.getS3FetchMessage());
    }

    @Test
    void testCreateConnectionFactoryWithManager() throws ResourceException {
        ConnectionManager cm = mock(ConnectionManager.class);
        Object cf = factory.createConnectionFactory(cm);
        assertNotNull(cf);
        assertTrue(cf instanceof AmazonSQSConnectionFactoryImpl);
    }

    @Test
    void testCreateConnectionFactoryWithoutManager() throws ResourceException {
        Object cf = factory.createConnectionFactory();
        assertNotNull(cf);
        assertTrue(cf instanceof AmazonSQSConnectionFactoryImpl);
    }

    @Test
    void testCreateManagedConnection() throws ResourceException {
        factory.setRegion("us-east-1"); 
        Subject subject = new Subject();
        ConnectionRequestInfo info = mock(ConnectionRequestInfo.class);
        ManagedConnection mc = factory.createManagedConnection(subject, info);
        assertNotNull(mc);
        assertTrue(mc instanceof AmazonSQSManagedConnection);
    }

    @Test
    void testMatchManagedConnections() throws ResourceException {
        ManagedConnection mc = mock(ManagedConnection.class);
        Set<ManagedConnection> set = new HashSet<>();
        set.add(mc);
        ManagedConnection matched = factory.matchManagedConnections(set, null, null);
        assertEquals(mc, matched);
    }

    @Test
    void testSetAndGetLogWriter() throws ResourceException {
        PrintWriter pw = new PrintWriter(System.out);
        factory.setLogWriter(pw);
        assertEquals(pw, factory.getLogWriter());
    }

    @Test
    void testEqualsAndHashCode() {
        AmazonSQSManagedConnectionFactory f1 = new AmazonSQSManagedConnectionFactory();
        AmazonSQSManagedConnectionFactory f2 = new AmazonSQSManagedConnectionFactory();

        f1.setAwsSecretKey("secret");
        f1.setAwsAccessKeyId("access");
        f1.setRegion("region");
        f1.setProfileName("profile");
        f1.setRoleArn("roleArn");
        f1.setRoleSessionName("session");

        f2.setAwsSecretKey("secret");
        f2.setAwsAccessKeyId("access");
        f2.setRegion("region");
        f2.setProfileName("profile");
        f2.setRoleArn("roleArn");
        f2.setRoleSessionName("session");

        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());

        f2.setRoleSessionName("different");
        assertNotEquals(f1, f2);
    }
}