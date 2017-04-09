# Apache Kafka
JCA Connector for Apache Kafka.

This consists of two modules the core api;
KafkaRAR

and the maven pom for p[ackaging the RAR file. If you need to use the api use this dependency;
```xml
    <dependency>
    <groupId>fish.payara.cloud.connectors.kafka</groupId>
    <artifactId>KafkaRARAPI</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>provided</scope>
    </dependency>
```