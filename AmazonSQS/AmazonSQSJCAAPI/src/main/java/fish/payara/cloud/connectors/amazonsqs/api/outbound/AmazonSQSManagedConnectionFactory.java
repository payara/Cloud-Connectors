/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2022 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnection;
import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnectionFactory;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.ConnectionDefinition;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

/**
 * @author Steve Millidge (Payara Foundation)
 */
@ConnectionDefinition(connection = AmazonSQSConnection.class,
        connectionFactory = AmazonSQSConnectionFactory.class,
        connectionFactoryImpl = AmazonSQSConnectionFactoryImpl.class,
        connectionImpl = AmazonSQSConnectionImpl.class
)
public class AmazonSQSManagedConnectionFactory implements ManagedConnectionFactory, Serializable {

    @ConfigProperty(description = "AWS Secret Key", type = String.class)
    private String awsSecretKey;

    @ConfigProperty(description = "AWS Access Key", type = String.class)
    private String awsAccessKeyId;

    @ConfigProperty(description = "Region hosting the queue", type = String.class)
    private String region;

    @ConfigProperty(description = "AWS Profile Name", type = String.class)
    private String profileName;

    private PrintWriter logger;

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.awsAccessKeyId = awsAccessKeyId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public AmazonSQSManagedConnectionFactory() {
    }



    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new AmazonSQSConnectionFactoryImpl(cxManager, this);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new AmazonSQSConnectionFactoryImpl(null, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return new AmazonSQSManagedConnection(subject, cxRequestInfo, this);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        // all connections are equal
        return (ManagedConnection) connectionSet.toArray()[0];
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        logger = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logger;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.awsSecretKey);
        hash = 97 * hash + Objects.hashCode(this.awsAccessKeyId);
        hash = 97 * hash + Objects.hashCode(this.region);
        hash = 97 * hash + Objects.hashCode(this.profileName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AmazonSQSManagedConnectionFactory other = (AmazonSQSManagedConnectionFactory) obj;
        if (!Objects.equals(this.awsSecretKey, other.awsSecretKey)) {
            return false;
        }
        if (!Objects.equals(this.awsAccessKeyId, other.awsAccessKeyId)) {
            return false;
        }
        if (!Objects.equals(this.region, other.region)) {
            return false;
        }
        if (!Objects.equals(this.profileName, other.profileName)) {
            return false;
        }
        return true;
    }



}
