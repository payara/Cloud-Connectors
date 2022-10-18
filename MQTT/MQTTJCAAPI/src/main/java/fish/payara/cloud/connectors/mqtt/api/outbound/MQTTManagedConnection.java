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
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class MQTTManagedConnection implements ManagedConnection, MQTTConnection {
    
    private final MQTTManagedConnectionFactory cf;
    private PrintWriter logWriter;
    private final Set<ConnectionEventListener> listeners;
    private final List<MQTTConnection> connectionHandles = new LinkedList<>();
    private final MqttClient theClient;
    private final boolean manuallyReconnectOnPublish;

    MQTTManagedConnection(MQTTManagedConnectionFactory aThis, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        cf = aThis;
        manuallyReconnectOnPublish = aThis.getManualReconnectOnPublish();
        listeners = new HashSet<>();
        MqttClientPersistence persistence = new MemoryPersistence();
        if (cf.getFilePersistance()) {
            persistence = new MqttDefaultFilePersistence(cf.getPersistenceDirectory());
        }
        try {
            String clientId = cf.getClientId();
            if (clientId == null) {
                clientId = MqttClient.generateClientId();
            }
            theClient = new MqttClient("tcp://localhost:1883", clientId,persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(cf.getAutomaticReconnect());
            options.setCleanSession(cf.getCleanSession());
            options.setServerURIs(cf.getServerURIs().split(","));
            options.setConnectionTimeout(cf.getConnectionTimeout());
            options.setKeepAliveInterval(cf.getKeepAliveInterval());
            
            if (cf.getPassword() != null ) {
                options.setPassword(cf.getPassword().toCharArray());
            }
            
            if (cf.getUserName() != null) {
                options.setUserName(cf.getUserName());
            }
            theClient.connect(options);
        } catch (MqttException ex) {
            throw new ResourceException("Unable to build the MQTT Client connection",ex);
        }
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        MQTTConnection newConn = new MQTTConnectionImpl(this);
        connectionHandles.add(newConn);
        return newConn;
    }

    @Override
    public void destroy() throws ResourceException {
        try {
            theClient.disconnect();
            theClient.close();
        } catch (MqttException ex) {
            throw new ResourceException("Unable to close the MQTT client connection", ex);
        }
    }

    @Override
    public void cleanup() throws ResourceException {
        connectionHandles.clear();
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        MQTTConnectionImpl impl = (MQTTConnectionImpl) connection;
        impl.setRealConnection(this);
        connectionHandles.add(impl);
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("Not supported yet."); 
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Not supported yet."); 
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() throws ResourceException {
                return "MQTT JCA Adapter";
            }

            @Override
            public String getEISProductVersion() throws ResourceException {
                return "1.0.0";
            }

            @Override
            public int getMaxConnections() throws ResourceException {
                return 0;
            }

            @Override
            public String getUserName() throws ResourceException {
                return "anonymous";
            }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }
    

    @Override
    public void publish(String topic, byte[] payload, int qos, boolean retained) throws ResourceException {
        try {
            boolean connected = theClient.isConnected();            
            if (!connected && manuallyReconnectOnPublish) {
                theClient.reconnect();
                connected = theClient.isConnected();
            }
            
            if (connected) {
                theClient.publish(topic, payload, qos, retained);
            } else {
                throw new ResourceException("Unable to send message to the MQTT Server we are disconnected");
            }
        } catch (MqttException ex) {
            throw new ResourceException("Unable to send message to MQTT Server",ex);
        }
    }

    void removeHandle(MQTTConnectionImpl handle) {
        connectionHandles.remove(handle);
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }

    @Override
    public void publish(String topic, MqttMessage message) throws ResourceException {
        try {
            boolean connected = theClient.isConnected();            
            if (!connected && manuallyReconnectOnPublish) {
                theClient.reconnect();
                connected = theClient.isConnected();
            }
            
            if (connected) {
                theClient.publish(topic, message);
            } else {
                throw new ResourceException("Unable to send message to the MQTT Server we are disconnected");                
            }
        } catch (MqttException ex) {
            throw new ResourceException("Unable to send message to MQTT Server",ex);
        }
    }

    @Override
    public void close() {
        try {
            destroy();
        } catch (ResourceException ex) {
            Logger.getLogger(MQTTManagedConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
