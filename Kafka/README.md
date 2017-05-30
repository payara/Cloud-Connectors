# Apache Kafka

These modules form the basis of the Apache Kafka JCA connector. The code is in three modules
* KafkaExample is an EJB jar module that shows a Timer Bean which sends a message periodically and an MDB that receives the message
* KafkaRAR is the bulk of the JCA code and the jar file which must be used as a provided dependency for any code using the JCA module
* KafkaRARPackaging is a maven module that assembles the rar file. The rar file should be deployed to your container.

To use the JCA adapter the KafkaRAR-<version>.rar should be deployed to your application server.

To deploy the JCA adapter on Payara Micro use the following commands.

```shell
java -jar payara-micro.jar --deploy kafka-rar-0.1.0.rar --deploy kafka-example-0.1.0.jar
```

## Inbound MDB
The KafkaExample module shows an example MDB that receives messages from a Kafka topic.
To receive messages you must implement the KafkaListener interface. 
```java
    public class KafkaMDB implements KafkaListener  
```

Also you must set the ActivationConfigProperty values suitable for your MDB. 

Valid properties are below. On Payara all properties can be replaced via System properties using the syntax `${system.property.name}` or environment variables using the syntax `${ENV=evironment.property.name}` or password aliases using the syntax `${ALIAS=alias.name}`;

|Config Property Name | Type | Default | Notes
|---------------------|------|---------|------
|bootstrapServersConfig | String | localhost:9092 | Kafka Servers to connect to
|clientId | String | KafkaJCAClient | Client ID of the Producer
|valueSerializer | String | org.apache.kafka.common.serialization.StringSerializer | Serializer class for value
|keySerializer | String | org.apache.kafka.common.serialization.StringSerializer | Serializer class for key
|bufferMemory | Long | 33554432 | The total bytes the producer can use to buffer messages
|acks | String | 1 | The number of acks the producer requires
|retries | Integer | 0 | The number of retries if there is a transient error
|batchSize | Long | 16384 | The producer will attempt to batch records together into fewer requests whenever multiple records are being sent to the same partition
|lingerMS | Long| 0 | The producer groups together any records that arrive in between request transmissions into a single batched request. 
|maxBlockMS | Long | 60000 | How long can send block 
|maxRequestSize | Long | 1048576 | Maximum size of request (bytes)
|receiveBufferBytes | Integer | 32768 | Receive Buffer (bytes)
|requestTimeout | Integer | 30000 | Request Timeout (ms)
|compression | String | "none" | Compression type of data sent
|connectionsMaxIdle | Long | 540000 | Close Idle Kafka Connections
|maxInflightConnections | Integer | 5 | Maximum unacknowledged requests to send before blocking
|metadataMaxAge| Long | 300000 | Period of time before a refresh of Metadata (ms)
|retryBackoff| Long | 100 | The amount of time to wait before attempting a retry (ms)
|reconnectBackoff| Long | 100 | The amount of time to wait before attempting a reconnection (ms)

Your MDB should contain one method annotated with `@OnRecord` and that method should take a single parameter of type `ConsumerRecord`. A specific set of topics to receive messages for can be specified `@OnRecord(topics={"test"})`

Your MDB can also receive messages in a batch using the @OnRecords annotation. 

A full skeleton MDB is shown below
```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "clientId", propertyValue = "testClient"),
    @ActivationConfigProperty(propertyName = "groupIdConfig", propertyValue = "testGroup"),
    @ActivationConfigProperty(propertyName = "topics", propertyValue = "test,test2"),
    @ActivationConfigProperty(propertyName = "bootstrapServersConfig", propertyValue = "localhost:9092"),    
    @ActivationConfigProperty(propertyName = "autoCommitInterval", propertyValue = "100"),    
    @ActivationConfigProperty(propertyName = "retryBackoff", propertyValue = "1000"),    
    @ActivationConfigProperty(propertyName = "keyDeserializer", propertyValue = "org.apache.kafka.common.serialization.StringDeserializer"),    
    @ActivationConfigProperty(propertyName = "valueDeserializer", propertyValue = "org.apache.kafka.common.serialization.StringDeserializer"),    
    @ActivationConfigProperty(propertyName = "pollInterval", propertyValue = "3000"),    
})
public class KafkaMDB implements KafkaListener {
    
    public KafkaMDB() {
    }
    
    @OnRecords( matchOtherMethods = true)
    public void getMessageTest2(ConsumerRecords records) {
        System.out.println("Bulk processing called with " + records.count() + " records");
    }
    
    @OnRecord( topics={"test"})
    public void getMessageTest(ConsumerRecord record) {
        System.out.println("Got record on topic test " + record);
    }
    
    @OnRecord( topics={"test2"})
    public void getMessageTest2(ConsumerRecord record) {
        System.out.println("Got record on topic test2 " + record);
    }
    
}
```

## Outbound messages sending
It is also possible to send messages to the Kafka topic using a defined connection factory. 
A full example of this is shown below;
```java
        try (KafkaConnection conn = factory.createConnection()) {
            conn.send(new ProducerRecord("test","hello","world"));
        } catch (Exception ex) {
        }

```

A Connection Factory can be configured using the standard JavaEE `ConnectionFactoryDefinition` annotation. Or can be configured using the administration console of your application server.
An example annotation defined connection factory is shown below;
```java
@ConnectionFactoryDefinition(name = "java:comp/env/KafkaConnectionFactory", 
  description = "Kafka Conn Factory", 
  interfaceName = "fish.payara.cloud.connectors.kafka.KafkaConnectionFactory", 
  resourceAdapter = "kafka-rar-0.2.0-SNAPSHOT", 
  minPoolSize = 2, 
  maxPoolSize = 2,
  transactionSupport = TransactionSupportLevel.NoTransaction,
  properties = {})
```

This connection factory can then be injected into any JavaEE component;
```java
    @Resource(lookup="java:comp/env/KafkaConnectionFactory")
    KafkaConnectionFactory factory;
```
