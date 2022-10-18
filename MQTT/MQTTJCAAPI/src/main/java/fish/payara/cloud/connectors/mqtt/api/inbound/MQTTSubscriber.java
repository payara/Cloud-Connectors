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

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.WorkManager;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class MQTTSubscriber implements IMqttMessageListener, MqttCallbackExtended {
    
    private MessageEndpointFactory factory;
    private MqttClient client;
    private WorkManager manager;
    private String topic;
    private int qos;
    private static final Logger LOGGER = Logger.getLogger(MQTTSubscriber.class.getName());

    MQTTSubscriber(MessageEndpointFactory endpointFactory, ActivationSpec spec, WorkManager manager) throws ResourceException {
        this.manager = manager;
        this.factory = endpointFactory;
        
        if (spec instanceof MQTTActivationSpec) {
            MQTTActivationSpec aspec = (MQTTActivationSpec) spec;
            topic = aspec.getTopicFilter();
            qos = aspec.getQos();
            try {
                String clientId = aspec.getClientId();
                if (clientId == null) {
                    clientId = MqttClient.generateClientId();
                }
                client = new MqttClient("tcp://localhost:1883",clientId);
                MqttConnectOptions options = new MqttConnectOptions();
                options.setServerURIs(aspec.getServerURIs().split(","));
                options.setAutomaticReconnect(aspec.getAutomaticReconnect());
                options.setCleanSession(aspec.getCleanSession());
                options.setConnectionTimeout(aspec.getConnectionTimeout());
                options.setKeepAliveInterval(aspec.getKeepAliveInterval());
                options.setMaxInflight(aspec.getMaxInflight());
                
                if (aspec.getPassword() != null) {
                options.setPassword(aspec.getPassword().toCharArray());
                }
                
                if (aspec.getUserName() != null) {
                    options.setUserName(aspec.getUserName());
                }
                client.connect(options);
                client.setCallback(this);

            } catch (MqttException ex) {
                throw new ResourceException("Unable to connection to the MQTT Server",ex);
            }
        }
        
    }
    
    void subscribe() throws ResourceException {
        try {       
            client.subscribe(topic, qos, this);
        } catch (MqttException ex) {
            throw new ResourceException("Unable to set up the topic subscription on the MQTT client", ex);
        }
    }

    void close() throws ResourceException {
        try {
            client.unsubscribe(topic);
            client.disconnect();
            client.close();
        } catch (MqttException ex) {
            throw new ResourceException("Unable to close the MQTT client", ex);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        manager.scheduleWork(new MQTTWork(topic, mm, factory));
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            try {
                LOGGER.log(Level.INFO, "Reconnection to {0} complete resubscribing for topic {1}", new Object[]{serverURI, topic});
                subscribe();
            } catch (ResourceException ex) {
                LOGGER.log(Level.SEVERE, "Problem resubscribing", ex);
            }
        }
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        LOGGER.warning("MQTT RAR Inbound lost connection to MQTT Broker");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        // do nothing
    }
    
}
