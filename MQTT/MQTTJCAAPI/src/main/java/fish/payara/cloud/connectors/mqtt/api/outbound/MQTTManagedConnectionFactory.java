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
package fish.payara.cloud.connectors.mqtt.api.outbound;

import fish.payara.cloud.connectors.mqtt.api.MQTTConnection;
import fish.payara.cloud.connectors.mqtt.api.MQTTConnectionFactory;
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
 *
 * @author Steve Millidge (Payara Foundation)
 */
@ConnectionDefinition (
    connection = MQTTConnection.class,
    connectionImpl = MQTTConnectionImpl.class,
    connectionFactory = MQTTConnectionFactory.class,
    connectionFactoryImpl = MQTTConnectionFactoryImpl.class
)
public class MQTTManagedConnectionFactory implements ManagedConnectionFactory, Serializable {
    
    private PrintWriter logWriter;
    
    @ConfigProperty(defaultValue = "tcp://localhost:1883", type = String.class, description = "Server URIs for connection, separated by ,")
    String serverURIs = "tcp://localhost:1883";
    
    @ConfigProperty(defaultValue = "false", type = Boolean.class, description = "Sets whether the client and server should remember state across reconnects ")
    Boolean cleanSession;
    
    @ConfigProperty(defaultValue = "true", type=Boolean.class, description = "Sets whether the client will automatically reconnect to the server if the connection is lost")
    Boolean automaticReconnect;

    @ConfigProperty(defaultValue = "false", type=Boolean.class, description = "Whether the client should use file persistence for unacked messages")
    Boolean filePersistance;

    @ConfigProperty(defaultValue = ".", type=String.class, description = "Directory to use for file persistence")
    String persistenceDirectory;
    
    @ConfigProperty(defaultValue = "30", type = Integer.class, description = "Sets the connection timeout value in seconds")
    Integer connectionTimeout;
    
    @ConfigProperty(defaultValue = "10", type = Integer.class, description = "Sets the maximum messages that can be sent without acknowledgements")
    Integer maxInflight;
    
    @ConfigProperty(defaultValue = "60", type = Integer.class, description = "Sets the keep alive interval in seconds")
    Integer keepAliveInterval;
    
    @ConfigProperty(type = String.class, description = "The user name for the connection")
    String userName;
    
    @ConfigProperty(type = String.class, description = "The password for the connection")
    String password;
    
    @ConfigProperty(type = String.class, description = "Client ID")
    String clientId;
    
    @ConfigProperty(type = Boolean.class, defaultValue="false", description = "Manually attempt reconnect on every publish call if disconnected")
    Boolean manualReconnectOnPublish;

    public Boolean getFilePersistance() {
        return filePersistance;
    }

    public void setFilePersistance(Boolean filePersistance) {
        this.filePersistance = filePersistance;
    }

    public String getPersistenceDirectory() {
        return persistenceDirectory;
    }

    public void setPersistenceDirectory(String persistenceDirectory) {
        this.persistenceDirectory = persistenceDirectory;
    }

    
    
    public String getServerURIs() {
        return serverURIs;
    }

    public void setServerURIs(String serverURIs) {
        this.serverURIs = serverURIs;
    }

    public Boolean getCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(Boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public Boolean getAutomaticReconnect() {
        return automaticReconnect;
    }

    public void setAutomaticReconnect(Boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getMaxInflight() {
        return maxInflight;
    }

    public void setMaxInflight(Integer maxInflight) {
        this.maxInflight = maxInflight;
    }

    public Integer getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(Integer keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }    

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new MQTTConnectionFactoryImpl(this, cxManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new MQTTConnectionFactoryImpl(this, null);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return new MQTTManagedConnection(this, subject, cxRequestInfo);
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

    public Boolean getManualReconnectOnPublish() {
        return manualReconnectOnPublish;
    }

    public void setManualReconnectOnPublish(Boolean manualReconnectOnPublish) {
        this.manualReconnectOnPublish = manualReconnectOnPublish;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.serverURIs);
        hash = 41 * hash + Objects.hashCode(this.cleanSession);
        hash = 41 * hash + Objects.hashCode(this.automaticReconnect);
        hash = 41 * hash + Objects.hashCode(this.filePersistance);
        hash = 41 * hash + Objects.hashCode(this.persistenceDirectory);
        hash = 41 * hash + Objects.hashCode(this.connectionTimeout);
        hash = 41 * hash + Objects.hashCode(this.maxInflight);
        hash = 41 * hash + Objects.hashCode(this.keepAliveInterval);
        hash = 41 * hash + Objects.hashCode(this.userName);
        hash = 41 * hash + Objects.hashCode(this.password);
        hash = 41 * hash + Objects.hashCode(this.clientId);
        hash = 41 * hash + Objects.hashCode(this.manualReconnectOnPublish);
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
        final MQTTManagedConnectionFactory other = (MQTTManagedConnectionFactory) obj;
        if (!Objects.equals(this.serverURIs, other.serverURIs)) {
            return false;
        }
        if (!Objects.equals(this.persistenceDirectory, other.persistenceDirectory)) {
            return false;
        }
        if (!Objects.equals(this.userName, other.userName)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        if (!Objects.equals(this.clientId, other.clientId)) {
            return false;
        }
        if (!Objects.equals(this.cleanSession, other.cleanSession)) {
            return false;
        }
        if (!Objects.equals(this.automaticReconnect, other.automaticReconnect)) {
            return false;
        }
        if (!Objects.equals(this.filePersistance, other.filePersistance)) {
            return false;
        }
        if (!Objects.equals(this.connectionTimeout, other.connectionTimeout)) {
            return false;
        }
        if (!Objects.equals(this.maxInflight, other.maxInflight)) {
            return false;
        }
        if (!Objects.equals(this.keepAliveInterval, other.keepAliveInterval)) {
            return false;
        }
        if (!Objects.equals(this.manualReconnectOnPublish, other.manualReconnectOnPublish)) {
            return false;
        }
        return true;
    }


    
}
