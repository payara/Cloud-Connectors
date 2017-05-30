# Amazon SQS

These modules form the basis of the Amazon Simple Queue Service JCA connector. The code is in three modules
* AmazonSQSExample is an EJB jar module that shows a Timer Bean which sends a message periodically and an MDB that receives the message
* AmazonSQSJCAAPI is the bulk of the JCA code and the jar file which must be used as a provided dependency for any code using the JCA module
* AmazonSQSRAR is a maven module that assembles the rar file. The rar file should be deployed to your container.

To use the JCA adapter the AmazonSQSRAR-<version>.rar should be deployed to your application server.

To deploy the JCA adapter on Payara Micro use the following commands.

The example uses a number of environment variables for connecting to your Amazon account.

```shell
   export accessKey="<your amazon access key>"
   export queueURL="<URL of the SQS queue in your amazon account>"
   export secretKey="<your amazon secret key>"
```

These environment variables must be set before running the example

```shell
java -jar payara-micro.jar --deploy amazon-sqs-rar-0.2.0-SNAPSHOT.rar amazon-sqs-example-0.2.0-SNAPSHOT.jar
```

## Inbound MDB
The AmazonSQSExample module shows an example MDB that receives messages from a queue.
To receive messages you must implement the AmazonSQSListener interface. 
```java
    public class ReceiveSQSMessage implements AmazonSQSListener 
```

Also you must set the ActivationConfigProperty values suitable for your MDB. 

Valid properties are below. On Payara all properties can be replaced via System properties using the syntax `${system.property.name}` or environment variables using the syntax `${ENV=evironment.property.name}` or password aliases using the syntax `${ALIAS=alias.name}`;

|Config Property Name | Type | Default | Notes
|---------------------|------|---------|------
|awsAccessKeyId | String | None | Must be set to the access key of your AWS account
|awsSecretKey | String | None | Must be set to the secret key of your AWS account
|queueURL | String | None | Must be set to the URL for an SQS queue
|region | String | None | Must be set to the AWS region name of your queue
|maxMessages | Integer | 10 | The maximum number of messages to download on a poll
|initialPollDelay | Integer | 1 | The delay (in seconds) before polling the queue after MDB activation (MDB only)
|pollInterval | Integer | 3 | How often should the adapter poll for messages (in seconds) (MDB Only)
|messageAttributeNames | String | All | The list of message attribute names that should be fetched with the message (MDB Only)
|attributeNames | String| All | The list of attribute names that should be fetched with the message (MDB Only)

Your MDB should contain one method annotated with `@OnSQSMessage` and that method should take a single parameter of type `com.amazonaws.services.sqs.model.Message`

A full skeleton MDB is shown below
```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "awsAccessKeyId", propertyValue = "${ENV=accessKey}"),
    @ActivationConfigProperty(propertyName = "awsSecretKey", propertyValue = "${ENV=secretKey}"),
    @ActivationConfigProperty(propertyName = "queueURL", propertyValue = "${ENV=queueURL}"),   
    @ActivationConfigProperty(propertyName = "pollInterval", propertyValue = "1"),    
    @ActivationConfigProperty(propertyName = "region", propertyValue = "eu-west-2")    
})
public class ReceiveSQSMessage implements AmazonSQSListener {

    @OnSQSMessage
    public void receiveMessage(Message message) {
        System.out.println("Got message " + message.getBody());
    }
}
```

## Outbound messages sending
It is also possible to send messages to the queue using a defined connection factory. 
A full example of this is shown below;
```java
        try (AmazonSQSConnection connection = factory.getConnection()) {
        connection.sendMessage(new SendMessageRequest("queueURL", "Hello World"));
        } catch (Exception e) {}

```

A Connection Factory can be configured using the standard JavaEE `ConnectionFactoryuDefinition` annotation. Or can be configured using the administration console of your application server.
An example annotation defined connection factory is shown below;
```java
@ConnectionFactoryDefinition(name = "java:comp/env/SQSConnectionFactory", 
  description = "SQS Conn Factory", 
  interfaceName = "fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSConnectionFactory", 
  resourceAdapter = "amazon-sqs-rar-0.2.0-SNAPSHOT", 
  minPoolSize = 2, 
  maxPoolSize = 2,
  transactionSupport = TransactionSupportLevel.NoTransaction,
  properties = {"awsAccessKeyId=${ENV=accessKey}",
                "awsSecretKey=${ENV=secretKey}",
                "region=eu-west-2"})
```

This connection factory can then be injected into any JavaEE component;
```java
    @Resource(lookup="java:comp/env/SQSConnectionFactory")
    AmazonSQSConnectionFactory factory;
```
