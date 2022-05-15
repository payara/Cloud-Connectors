# Payara Cloud Connectors
[![Java CI](https://github.com/payara/Cloud-Connectors/actions/workflows/maven.yml/badge.svg)](https://github.com/payara/Cloud-Connectors/actions/workflows/maven.yml)

Payara Cloud Connectors is a project to provide JavaEE standards based connectivity 
to common Cloud infrastructure. Utilising JCA we provide connectivity to many different 
services provided by the leading cloud providers and open source technologies. Payara Cloud Connectors enable the creation of Cloud Native applications using JavaEE apis with the ability to build Event Sourcing and Message Driven architectures simply on public clouds. 

Payara Cloud Connectors are proven to work with Payara Server and Payara Micro 172+. The JCA connectors should work on other Java EE 8 application servers as they do not use any Payara specific code.

Currently we have JCA adapters for;
* Apache Kafka - sending messages using a Connection Factory and receiving messages via an MDB
* Amazon SQS - sending messages using a Connection Factory and receiving messages via an MDB to Amazon Simple Queue Service queues.
* MQTT - sending messages using a Connection Factory and receiving messages via an MDB to an MQTT broker or to IOT hubs that support MQTT.
* Azure Service Bus - Sending and receiving messages to/from Azure Service Bus Queues using ConnectionFactory via MDB

**ATTENTION**: Support for this repository is handled in the [Ecosystem Support repository](https://github.com/payara/ecosystem-support)

## Why Use JCA

One of the benefits of using these JCA adapters rather than crafting your own clients, using the standard apis for the messaging technologies, is that the JCA adapters are fully integrated into your Java EE environment.
That means your JavaEE application can use familiar JavaEE constructs such as Message Driven Beans and Connection Factories. Using MDBs to receive messages means that any threads are automatically provided via the 
Java EE application server which means they can take advantage of Container Transactions, Security, integration with EJBs, CDI and the full range of Java EE components.
Connection factories for outbound messaging also benefit from connection pooling and configuration via the administration console or via annotations in your code.
