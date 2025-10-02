package fish.payara.cloud.connectors.mqtt.api.inbound;

import fish.payara.cloud.connectors.mqtt.api.OnMQTTMessage;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.*;

class MQTTWorkTest {

    @Test
    void testRunInvokesAnnotatedMethodAndLifecycle() throws Exception {
        // Dummy endpoint class with annotated method
        class DummyEndpoint implements MessageEndpoint {
            boolean beforeCalled = false;
            boolean afterCalled = false;
            boolean released = false;
            boolean invoked = false;

            @OnMQTTMessage
            public void handle(String topic, MqttMessage msg) {
                invoked = true;
            }

            @Override
            public void beforeDelivery(Method method) {
                beforeCalled = true;
            }

            @Override
            public void afterDelivery() {
                afterCalled = true;
            }

            @Override
            public void release() {
                released = true;
            }
        }

        DummyEndpoint endpoint = spy(new DummyEndpoint());
        MessageEndpointFactory factory = mock(MessageEndpointFactory.class);
        when(factory.getEndpointClass()).thenAnswer(invocation -> (Class<?>) DummyEndpoint.class);
        when(factory.createEndpoint(null)).thenReturn(endpoint);

        MqttMessage mqttMessage = new MqttMessage("payload".getBytes());
        MQTTWork work = new MQTTWork("topic/test", mqttMessage, factory);

        work.run();

        verify(endpoint).beforeDelivery(any(Method.class));
        verify(endpoint).afterDelivery();
        verify(endpoint).release();
        assertTrue(endpoint.invoked);
        assertTrue(endpoint.beforeCalled);
        assertTrue(endpoint.afterCalled);
        assertTrue(endpoint.released);
    }

    @Test
    void testRunWithNoAnnotatedMethodDoesNothing() {
        class NoAnnotationEndpoint implements MessageEndpoint {
            @Override public void beforeDelivery(Method method) {}
            @Override public void afterDelivery() {}
            @Override public void release() {}
            public void notHandle(String topic, MqttMessage msg) {}
        }

        MessageEndpointFactory factory = mock(MessageEndpointFactory.class);
        when(factory.getEndpointClass()).thenAnswer(invocation -> (Class<?>) NoAnnotationEndpoint.class);

        MQTTWork work = new MQTTWork("topic/test", new MqttMessage("payload".getBytes()), factory);

        // Should not throw or call any endpoint methods
        work.run();
        // No exception expected
    }

    @Test
    void testReleaseCallsReleaseEndpoint() throws Exception {
        class DummyEndpoint implements MessageEndpoint {
            boolean released = false;
            @OnMQTTMessage
            public void handle(String topic, MqttMessage msg) {}
            @Override public void beforeDelivery(Method method) {}
            @Override public void afterDelivery() {}
            @Override public void release() { released = true; }
        }

        DummyEndpoint endpoint = spy(new DummyEndpoint());
        MessageEndpointFactory factory = mock(MessageEndpointFactory.class);
        when(factory.getEndpointClass()).thenAnswer(invocation -> (Class<?>) DummyEndpoint.class);
        when(factory.createEndpoint(null)).thenReturn(endpoint);

        MQTTWork work = new MQTTWork("topic/test", new MqttMessage("payload".getBytes()), factory);
        work.run();
        work.release();

        verify(endpoint, atLeastOnce()).release();
    }
}