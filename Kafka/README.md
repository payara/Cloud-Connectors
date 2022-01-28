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

To deploy independently on Payara Server ensure the Connector Classloading Policy os set to GLOBAL in the Connector Service of your instance configuration.

## Resource Adapter Properties 

Valid properties are below. On Payara all properties in annotations can be replaced via System properties using the syntax `${system.property.name}` or environment variables using the syntax `${ENV=evironment.property.name}` or password aliases using the syntax `${ALIAS=alias.name}`;

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
|commitEachPoll| Boolean | false | If commitEachPoll is set to true a commitSynch will occur after MDBs have processed the records from a poll. Note multiple MDB calls may be made as a result of a single poll (For MDBs Only)
|enableAutoCommit | Boolean | true | Enables autocommit on the Kafka Consumer (MDBs Only)
|pollInterval | Long | 1000 | How often the MDB Consumer should poll Kafka for new records (MDBs only)
|autoCommitInterval | Long | None | Interval before autoCommit is sent to the broker (ms) (MDBs only)
|useSynchMode | Boolean | false | In synch mode a single MDB instance will consume all the records in sequence on a single thread (MDBs only)

## Inbound Processing (MDBs)

For inbound processing the Resource Adapter has two modes.

### Synch Mode
Synch mode is an inbound processing mode whereby the resource adapter uses a single work manager thread
to poll the KafkaConsumer and process all the inbound records. Synch mode uses a single thread and MDB instance
to process all the records received from Kaka on the configured topic.

Synch mode is enabled on an MDB by setting
```java
    @ActivationConfigProperty(propertyName = "useSynchMode", propertyValue = "true")
```

### Asynch Mode
Asynch mode is the default. In this mode the resource adapter continually submits a Work Manager
work instance to poll Kafka. When records are received a second Work instance is submitted to the 
Work Manager to process all the records from the poll. Therefore record processing occurs on a different thread to 
than polling for records. All records from the poll are processed by a single MDB instance.

### Example MDB

The KafkaExample module shows an example MDB that receives messages from a Kafka topic.
To receive messages you must implement the KafkaListener interface. 
```java
    public class KafkaMDB implements KafkaListener  
```

Also you must set the ActivationConfigProperty values suitable for your MDB.

Your MDB should contain one method annotated with `@OnRecord` and that method should take a single parameter of type `ConsumerRecord`. A specific set of topics to receive messages for can be specified `@OnRecord(topics={"test"})`

Your MDB can also receive messages in a batch using the @OnRecords annotation. 

A full skeleton MDB is shown below
```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "clientId", propertyValue = "testClient"),
    @ActivationConfigProperty(propertyName = "groupIdConfig", propertyValue = "testGroup"),
    @ActivationConfigProperty(propertyName = "topics", propertyValue = "test,test2"),
    @ActivationConfigProperty(propertyName = "bootstrapServersConfig", propertyValue = "localhost:9092"),    
    @ActivationConfigProperty(propertyName = "enableAutoCommit", propertyValue = "true"),    
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
  interfaceName = "fish.payara.cloud.connectors.kafka.api.KafkaConnectionFactory", 
  resourceAdapter = "kafka-rar-0.7.0", 
  minPoolSize = 2, 
  maxPoolSize = 2,
  transactionSupport = TransactionSupportLevel.NoTransaction,
  properties = {})
```

additional `KafkaProducer` properties can be set by using the `properties` section of the `ConnectionFactoryDefinition`

This connection factory can then be injected into any JavaEE component;
```java
    @Resource(lookup="java:comp/env/KafkaConnectionFactory")
    KafkaConnectionFactory factory;
```
