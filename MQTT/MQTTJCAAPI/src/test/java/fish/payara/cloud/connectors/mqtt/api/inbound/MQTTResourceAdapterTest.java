package fish.payara.cloud.connectors.mqtt.api.inbound;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MQTTResourceAdapterTest {

    private MQTTResourceAdapter adapter;
    private BootstrapContext bootstrapContext;
    private MessageEndpointFactory endpointFactory;
    private ActivationSpec activationSpec;

    @BeforeEach
    void setUp() {
        adapter = new TestMQTTResourceAdapter();
        bootstrapContext = mock(BootstrapContext.class);
        endpointFactory = mock(MessageEndpointFactory.class);
        activationSpec = mock(ActivationSpec.class);
    }

    @Test
    void testStartSetsContext() throws Exception {
        adapter.start(bootstrapContext);
        var contextField = MQTTResourceAdapter.class.getDeclaredField("context");
        contextField.setAccessible(true);
        assertEquals(bootstrapContext, contextField.get(adapter));
    }

    @Test
    void testGetXAResourcesReturnsNull() throws Exception {
        XAResource[] result = adapter.getXAResources(new ActivationSpec[0]);
        assertNull(result);
    }

    @Test
    void testEqualsAndHashCode() {
        MQTTResourceAdapter adapter2 = new MQTTResourceAdapter();
        assertEquals(adapter, adapter);
        assertNotEquals(adapter, adapter2); // default equals uses reference
        assertEquals(adapter.hashCode(), adapter.hashCode());
    }

    static class TestMQTTResourceAdapter extends MQTTResourceAdapter {
        protected MQTTSubscriber createSubscriber(MessageEndpointFactory endpointFactory, ActivationSpec spec, jakarta.resource.spi.work.WorkManager workManager) {
            try {
                MQTTSubscriber subscriber = spy(new MQTTSubscriber(endpointFactory, spec, workManager));
                var clientField = MQTTSubscriber.class.getDeclaredField("client");
                clientField.setAccessible(true);
                clientField.set(subscriber, mock(org.eclipse.paho.client.mqttv3.MqttClient.class));
                doNothing().when(subscriber).subscribe();
                doNothing().when(subscriber).close();
                return subscriber;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}