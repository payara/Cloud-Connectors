# Change Log for the MQTT JCA Connector

## 0.5.0
* new `ConnectionFactory` property `manualReconnectOnPublish`
* Connector inbound subsystem now resubscribes for topics when it detects a reconnect
* Connector inbound on undeploy connector unsubscribes MDBs from topics before closing MQTT client this prevent spurious exceptions for in-flight messages
* Outbound doesn't try to publish a message if not connected

`manualReconnectOnPublish` Every time publish is called if the connection is disconnected from the broker a reconnect call will be attempted. 
Note depending on your publish frequency this could lead to many reconnect calls in short succession.
Use this setting if you want rapid reconnection rather than waiting for autoreconnect if configured. 

## 0.4.0
* upgrade Paho client to 1.2.1

# 0.3.0
* Resource Adapter now provides `equals()` and `hashcode()` to better support WildFly

# 0.2.0
* new examples for sending and receiving messages

# 0.1.0
Initial Release


