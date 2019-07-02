# Change Log for Kafka Resource Adapter

## 0.5.0

Reimplemented the JCA adapter and now has two modes of operation
Further testing and bug fixing for reconnects, redeploys of MDBs and the RAR.

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

### commitOnEachPoll ActivationCongigProperty

There is a new inbound MDB Activation Config Property `commitOnEachPoll` when this property is set
the KafkaConsumer will commit the offset after each successful poll of Kafka. Be aware that a single poll may receive multiple
`ConsumerRecord` objects and the commit will occur after all records are processed.

Without this set the KafkaConsumer will commit the offset periodically based on the Activation Config Properties of the MDB.

### KafkaConnection now supports metrics

The outbound `KafkaConnection` api now supports retrieving the metrics for the associated `KafkaProducer`

## 0.4.0

Kafka Client API bumped to 2.2.1
Performance increase whereby method lookups for the MDB class are cached rather than performed for each Record
Connection Factory now explicitly states it does not support transactions

## 0.3.0

Resource Adapter now implements `equals()` and `hashcode()` for better WildFly support

## 0.2.0

No changes

## 0.1.0

First Release

