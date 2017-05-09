# Payara Cloud Connectors

Payara Cloud Connectors is a project to provide JavaEE standards based connectivity 
to Cloud infrastructure. Utilising JCA we will provide connectivity to many different 
services provided by the leading cloud providers and open source technologies.

Payara Cloud Connectors are proven to work with Payara Server and Payara Micro 172+

Currently we have JCA adapters for;
* Apache Kafka - sending messages using a Connection Factory and receiving messages via an MDB
* Amazon SQS - sending messages using a Connection Factory and receiving messages via an MDB
* MQTT - sending messages using a Connection Factory and receiving messages via an MDB

## Why Use JCA

One of the benefits of using these JCA adapters rather than crafting your own clients using the standard apis for the support technologies is that the JCA adapters are fully integrated into your Java EE environment.
That means your JavaEE application can use familiar JavaEE constructs such as Message Driven Beans and Connection Factories. Using MDB means that any threads are automatically provided via the 
Java EE application server which means they can take advantage of Container Transactions, Security, integration with EJBs, CDI and the full range of Java EE components.
Connection factoryies for outbound messaging also benefit from connection pooling and configuration via the administration console. 