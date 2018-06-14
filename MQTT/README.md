# MQTT JCA Adapter

These modules form the basis of the MQTT JCA connector. The code is in three modules
* MQTTExample is an EJB jar module that shows a Timer Bean which sends a message periodically to an MQTT topic and an MDB that receives the message
* MQTTJCAAPI is the bulk of the JCA code and the jar file which must be used as a provided dependency for any code using the JCA module
* MQTTRAR is a maven module that assembles the rar file. The rar file should be deployed to your container.

To use the JCA adapter the MQTTRAR-<version>.rar should be deployed to your application server.

To deploy the JCA adapter on Payara Micro use the following commands.

You will first need to install an MQTT broker for example Mosquito in Linux.

```shell
java -jar payara-micro.jar --deploy mqtt-rar-0.3.0.rar mqtt-example-0.3.0.jar
```

## Inbound MDB
The MQTTExample module shows an example MDB that receives messages from a queue.
To receive messages you must implement the MQTTListener interface. 
```java
 public class MQTTMDB implements MQTTListener  
```

Also you must set the ActivationConfigProperty values suitable for your MDB. 

Valid properties for MDBs and Connection Factories are below. On Payara all properties can be replaced via System properties using the syntax `${system.property.name}` or environment variables using the syntax `${ENV=environment.property.name}` or password aliases using the syntax `${ALIAS=alias.name}`;

|Config Property Name | Type | Default | Notes
|---------------------|------|---------|------
|serverURIs | String | tcp://localhost:1883 | Server URIs for connection, separated by ,
|cleanSession | Boolean | false | Sets whether the client and server should remember state across reconnects 
|automaticReconnect | Boolean | true | Sets whether the client will automatically reconnect to the server if the connection is lost
|filePersistance | Boolean | false | Whether the client should use file persistence for unacked messages
|persistenceDirectory | String | . | Directory to use for file persistence
|connectionTimeout | Integer | 30 | Sets the connection timeout value in seconds
|maxInflight | Integer | 10 | Sets the maximum messages that can be sent without acknowledgements
|keepAliveInterval | Integer | 60 | Sets the keep alive interval in seconds
|userName | String | None | The user name for the connection. 
|password | String  | None | The password for the connection. 
|topicFilter| String | None | Topic Filter (For MDBs only)
|qos| String | 0 | Quality of Service for the subscription (For MDBs only)

Your MDB should contain one method annotated with `@OnMQTTMessage` and that method should take a two parameters one of type String for the topic and  `MqttMessage`. 

A full skeleton MDB is shown below
```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "topicFilter", propertyValue = "test")    
})
public class MQTTMDB implements MQTTListener {

    @OnMQTTMessage
    public void messageArrived(String topic, MqttMessage message) {
        System.out.println("Received message on topic " + topic + " " + new String(message.getPayload()));
    }
    
}
```

## Outbound messages sending
It is also possible to send messages to the topic using a defined connection factory. 
A full example of this is shown below;
```java
        try (MQTTConnection conn = factory.getConnection()) {
            conn.publish("test", "{\"test\": \"Hello World\"}".getBytes(), 0, false);
        } catch (Exception e) {
            
        }
```

A Connection Factory can be configured using the standard JavaEE `ConnectionFactoryDefinition` annotation. Or can be configured using the administration console of your application server.
An example annotation defined connection factory is shown below;
```java
@ConnectionFactoryDefinition(name = "java:comp/env/MQTTConnectionFactory", 
  description = "MQTT Conn Factory", 
  interfaceName = "fish.payara.cloud.connectors.mqtt.api.MQTTConnectionFactory", 
  resourceAdapter = "mqtt-rar-0.3.0", 
  minPoolSize = 2, 
  maxPoolSize = 2,
  transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction,
  properties = {"cleanSession=true"})
```

This connection factory can then be injected into any JavaEE component;
```java
    @Resource(lookup="java:comp/env/MQTTConnectionFactory")
    MQTTConnectionFactory factory;
```
