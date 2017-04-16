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
package fish.payara.cloud.connectors.mqtt.api.inbound;

import fish.payara.cloud.connectors.mqtt.api.MQTTListener;
import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Activation(messageListeners = MQTTListener.class)
public class MQTTActivationSpec implements ActivationSpec {
    
    private ResourceAdapter adapter;
    
    @ConfigProperty(defaultValue = "tcp://localhost:1883", type = String.class, description = "Array of Server URIs for connection")
    String serverURIs = "tcp://localhost:1883";
    
    @ConfigProperty(defaultValue = "false", type = Boolean.class, description = "Sets whether the client and server should remember state across reconnects ")
    Boolean cleanSession = false;
    
    @ConfigProperty(defaultValue = "true", type=Boolean.class, description = "Sets whether the client will automatically reconnect to the server if the connection is lost")
    Boolean automaticReconnect = true;

    @ConfigProperty(defaultValue = "false", type=Boolean.class, description = "Whether the client should use file persistence for unacked messages")
    Boolean filePersistance = false;

    @ConfigProperty(defaultValue = ".", type=String.class, description = "Directory to use for file persistence")
    String persistenceDirectory = ".";
    
    @ConfigProperty(defaultValue = "30", type = Integer.class, description = "Sets the connection timeout value in seconds")
    Integer connectionTimeout = 30;
    
    @ConfigProperty(defaultValue = "10", type = Integer.class, description = "Sets the maximum messages that can be sent without acknowledgements")
    Integer maxInflight = 10;
    
    @ConfigProperty(defaultValue = "60", type = Integer.class, description = "Sets the keep alive interval in seconds")
    Integer keepAliveInterval = 60;
    
    @ConfigProperty(type = String.class, description = "The user name for the connection")
    String userName;
    
    @ConfigProperty(type = String.class, description = "The password for the connection")
    String password;

    @ConfigProperty(type = String.class, description = "Topic Filter")
    String topicFilter;
    
    @ConfigProperty(defaultValue = "0", type = Integer.class, description = "Quality of Service for the subscription")
    Integer qos = 0;
    
    @Override
    public void validate() throws InvalidPropertyException {
        if (topicFilter == null) {
            throw new InvalidPropertyException("Topic Filter can not be null");
        }
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public void setTopicFilter(String topicFilter) {
        this.topicFilter = topicFilter;
    }

    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    public String getServerURIs() {
        return serverURIs;
    }

    public void setServerURIs(String serverURI) {
        this.serverURIs = serverURI;
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

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        adapter= ra;
    }
    
}
