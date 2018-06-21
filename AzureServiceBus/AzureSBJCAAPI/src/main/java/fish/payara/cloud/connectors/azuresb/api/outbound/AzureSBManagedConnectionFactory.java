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
package fish.payara.cloud.connectors.azuresb.api.outbound;

import fish.payara.cloud.connectors.azuresb.api.AzureSBConnection;
import fish.payara.cloud.connectors.azuresb.api.AzureSBConnectionFactory;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@ConnectionDefinition (
    connection = AzureSBConnection.class,
    connectionImpl = AzureSBConnectionImpl.class,
    connectionFactory = AzureSBConnectionFactory.class,
    connectionFactoryImpl = AzureSBConnectionFactoryImpl.class
)
public class AzureSBManagedConnectionFactory implements ManagedConnectionFactory, Serializable {
    
    @ConfigProperty(defaultValue = "", type = String.class, description = "Namespace")
    private String nameSpace = "";
    
    @ConfigProperty(defaultValue = "", type = String.class, description = "SAS Key Name")
    private String sasKeyName;
    
    @ConfigProperty(defaultValue = "", type=String.class, description = "The SAS Key")
    private String sasKey;
    
    @ConfigProperty(defaultValue = "", type=String.class, description = "Queue Name")
    private String queueName;
    
    @ConfigProperty(defaultValue = "5", type=Integer.class, description = "Timeout")
    private Integer timeOut;
    
    private PrintWriter logWriter;

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new AzureSBConnectionFactoryImpl(this, cxManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new AzureSBConnectionFactoryImpl(this, null);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return new AzureSBManagedConnection(this,subject,cxRequestInfo);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return (ManagedConnection) connectionSet.toArray()[0];
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getSasKeyName() {
        return sasKeyName;
    }

    public void setSasKeyName(String sasKeyName) {
        this.sasKeyName = sasKeyName;
    }

    public String getSasKey() {
        return sasKey;
    }

    public void setSasKey(String sasKey) {
        this.sasKey = sasKey;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public Integer getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Integer timeOut) {
        this.timeOut = timeOut;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.nameSpace);
        hash = 97 * hash + Objects.hashCode(this.sasKeyName);
        hash = 97 * hash + Objects.hashCode(this.sasKey);
        hash = 97 * hash + Objects.hashCode(this.queueName);
        hash = 97 * hash + Objects.hashCode(this.timeOut);
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
        final AzureSBManagedConnectionFactory other = (AzureSBManagedConnectionFactory) obj;
        if (!Objects.equals(this.nameSpace, other.nameSpace)) {
            return false;
        }
        if (!Objects.equals(this.sasKeyName, other.sasKeyName)) {
            return false;
        }
        if (!Objects.equals(this.sasKey, other.sasKey)) {
            return false;
        }
        if (!Objects.equals(this.queueName, other.queueName)) {
            return false;
        }
        if (!Objects.equals(this.timeOut, other.timeOut)) {
            return false;
        }
        return true;
    }
    
    
    
}
