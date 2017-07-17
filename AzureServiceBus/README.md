# Azure Service Bus

These modules form the basis of the Azure Service Bus Queues JCA connector. The code is in three modules
* AzureSBExample is an EJB jar module that shows a Timer Bean which sends a message periodically and an MDB that receives the message from a Service Bus Queue
* AzureSBJCAAPI is the bulk of the JCA code and the jar file which must be used as a provided dependency for any code using the JCA module
* AzureSBRAR is a maven module that assembles the rar file. The rar file should be deployed to your container.

To use the JCA adapter the AzureSBRAR-<version>.rar should be deployed to your application server.

To deploy the JCA adapter on Payara Micro use the following commands.

```shell
java -jar payara-micro.jar --deploy azure-sb-rar-0.2.0-SNAPSHOT.rar azure-sb-example-0.2.0-SNAPSHOT.jar
```

## Inbound MDB
The AzureSBExample module shows an example MDB that receives messages from a queue.
To receive messages you must implement the AzureSBListener interface. 
```java
    public class AzureSBMDB implements AzureSBListener   
```

Also you must set the ActivationConfigProperty values suitable for your MDB. 

Valid properties for the connection factory and MDBs are below. On Payara all properties can be replaced via System properties using the syntax `${system.property.name}` or environment variables using the syntax `${ENV=evironment.property.name}` or password aliases using the syntax `${ALIAS=alias.name}`;

|Config Property Name | Type | Default | Notes
|---------------------|------|---------|------
|sasKeyName | String | none | The SAS Key Name defined in your Service Bus namespace
|sasKey | String | none | The SAS Key. Environment variable replacement can be used so that this is not exposed in the code
|nameSpace | String | none | The Azure namespace of your Service Bus
|queueName | String | none | The Queue Name (MDB property only)
|initialPollDelay | Integer | 1 | The Initial Poll Delay (in s) before the Adapter starts polling for messages after deployment (MDB Property Only)
|pollInterval | Integer | 1 | The Poll interval (in s). This is how often the MDB polls for new messages (MDB Property Only)
|pollTimeout  | Integer | 1 | The Poll Timeout (in s). This is hopw long the MDB should wait for messages in a single poll (MDB Property Only)

Your MDB should contain one method annotated with `@OnAzureSBMessage` and that method should take a single parameter of type `BrokeredMessage`. 

A full skeleton MDB is shown below
```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "nameSpace", propertyValue = "payara"),    
    @ActivationConfigProperty(propertyName = "sasKeyName", propertyValue = "RootManageSharedAccessKey"),   
    @ActivationConfigProperty(propertyName = "sasKey", propertyValue = "${ENV=sasKey}"),   
    @ActivationConfigProperty(propertyName = "queueName", propertyValue = "testq")    
})
public class AzureSBMDB implements AzureSBListener {

    @OnAzureSBMessage
    public void receiveMessage(BrokeredMessage message) {
        System.out.println("Received Message " + message.getBody());
    }
}
```

## Outbound messages sending
It is also possible to send messages to the queue using a defined connection factory. 
A full example of this is shown below;
```java
        try (AzureSBConnection connection = factory.getConnection()) {
            connection.sendMessage("testq", new BrokeredMessage("Hello World"));
            System.out.println("Sent message");
        } catch (Exception e) {
        }
```

A Connection Factory can be configured using the standard JavaEE `ConnectionFactoryDefinition` annotation. Or can be configured using the administration console of your application server.
An example annotation defined connection factory is shown below;
```java
@ConnectionFactoryDefinition(name = "java:comp/env/AzureSBConnectionFactory",
        description = "Azure SB Conn Factory",
        interfaceName = "fish.payara.cloud.connectors.azuresb.api.AzureSBConnectionFactory",
        resourceAdapter = "azure-sb-rar-0.2.0-SNAPSHOT",
        minPoolSize = 2, maxPoolSize = 2,
        transactionSupport = TransactionSupportLevel.NoTransaction,
        properties = {"nameSpace=payara",
            "sasKeyName=RootManageSharedAccessKey",
            "sasKey=${ENV=sasKey}"
        })
```

This connection factory can then be injected into any JavaEE component;
```java
    @Resource(lookup = "java:comp/env/AzureSBConnectionFactory")
    AzureSBConnectionFactory factory;
```
