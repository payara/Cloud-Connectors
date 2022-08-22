/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.connectors.mqtt.example;

import fish.payara.cloud.connectors.mqtt.api.MQTTConnection;
import fish.payara.cloud.connectors.mqtt.api.MQTTConnectionFactory;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Schedule;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.spi.TransactionSupport;

/**
 *
 * @author martin
 */
@ConnectionFactoryDefinition(name = "java:comp/env/MQTTConnectionFactory", 
  description = "MQTT Conn Factory", 
  interfaceName = "fish.payara.cloud.connectors.mqtt.api.MQTTConnectionFactory", 
  resourceAdapter = "mqtt-rar-0.9.0-SNAPSHOT", 
  minPoolSize = 2, 
  maxPoolSize = 2,
  transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction,
  properties = {"cleanSession=true","automaticReconnect=true"})
@Stateless
public class TimerSend {

    @Resource(lookup="java:comp/env/MQTTConnectionFactory")
    MQTTConnectionFactory factory;

    
    @Schedule(second = "*/1", hour="*", minute="*", persistent = false)   
    public void sendMessage() {
        try (MQTTConnection conn = factory.getConnection()) {
            conn.publish("test", "{\"test\": \"Hello World\"}".getBytes(), 0, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
