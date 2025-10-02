package fish.payara.cloud.connectors.mqtt.api.inbound;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MQTTResourceAdapterTest {

    private MQTTResourceAdapter adapter;
    private BootstrapContext bootstrapContext;
    private MessageEndpointFactory endpointFactory;
    private ActivationSpec activationSpec;
    private MQTTSubscriber subscriber;

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

    @Disabled("Cannot inject mock client before subscribe() is called; requires production code change")
    @Test
    void testStopClosesSubscribers() throws Exception {
        var workManager = mock(jakarta.resource.spi.work.WorkManager.class);
        when(bootstrapContext.getWorkManager()).thenReturn(workManager);

        adapter.start(bootstrapContext);
        adapter.endpointActivation(endpointFactory, activationSpec);

        var factoriesField = MQTTResourceAdapter.class.getDeclaredField("registeredFactories");
        factoriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var factories = (java.util.Map<MessageEndpointFactory, MQTTSubscriber>) factoriesField.get(adapter);

        MQTTSubscriber subscriber = factories.get(endpointFactory);
        assertNotNull(subscriber);

        // Inject mock client and stub close
        var clientField = MQTTSubscriber.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(subscriber, mock(org.eclipse.paho.client.mqttv3.MqttClient.class));
        MQTTSubscriber spySubscriber = spy(subscriber);
        factories.put(endpointFactory, spySubscriber);
        doNothing().when(spySubscriber).close();

        adapter.stop();

        verify(spySubscriber).close();
        assertTrue(factories.isEmpty());
    }

    @Disabled("Cannot inject mock client before subscribe() is called; requires production code change")
    @Test
    void testEndpointActivationAddsSubscriberAndSubscribes() throws Exception {
        var workManager = mock(jakarta.resource.spi.work.WorkManager.class);
        when(bootstrapContext.getWorkManager()).thenReturn(workManager);

        // Spy the adapter to intercept endpointActivation
        MQTTResourceAdapter spyAdapter = spy(new MQTTResourceAdapter());
        spyAdapter.start(bootstrapContext);

        // Intercept the creation of MQTTSubscriber
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            // After activation, inject mock client
            var factoriesField = MQTTResourceAdapter.class.getDeclaredField("registeredFactories");
            factoriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var factories = (java.util.Map<MessageEndpointFactory, MQTTSubscriber>) factoriesField.get(spyAdapter);
            MQTTSubscriber subscriber = factories.get(endpointFactory);
            var clientField = MQTTSubscriber.class.getDeclaredField("client");
            clientField.setAccessible(true);
            clientField.set(subscriber, mock(org.eclipse.paho.client.mqttv3.MqttClient.class));
            return result;
        }).when(spyAdapter).endpointActivation(any(), any());

        // Now call endpointActivation (mock client will be injected before subscribe)
        spyAdapter.endpointActivation(endpointFactory, activationSpec);

        // Now you can safely call subscribe
        var factoriesField = MQTTResourceAdapter.class.getDeclaredField("registeredFactories");
        factoriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var factories = (java.util.Map<MessageEndpointFactory, MQTTSubscriber>) factoriesField.get(spyAdapter);
        MQTTSubscriber subscriber = factories.get(endpointFactory);
        assertNotNull(subscriber);
        assertDoesNotThrow(subscriber::subscribe);
    }

    @Disabled("Cannot inject mock client before subscribe() is called; requires production code change")
    @Test
    void testEndpointDeactivationRemovesAndClosesSubscriber() throws Exception {
        var workManager = mock(jakarta.resource.spi.work.WorkManager.class);
        when(bootstrapContext.getWorkManager()).thenReturn(workManager);

        adapter.start(bootstrapContext);
        adapter.endpointActivation(endpointFactory, activationSpec);

        var factoriesField = MQTTResourceAdapter.class.getDeclaredField("registeredFactories");
        factoriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var factories = (java.util.Map<MessageEndpointFactory, MQTTSubscriber>) factoriesField.get(adapter);

        MQTTSubscriber subscriber = factories.get(endpointFactory);
        assertNotNull(subscriber);

        // Inject mock client and stub close
        var clientField = MQTTSubscriber.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(subscriber, mock(org.eclipse.paho.client.mqttv3.MqttClient.class));
        MQTTSubscriber spySubscriber = spy(subscriber);
        factories.put(endpointFactory, spySubscriber);
        doNothing().when(spySubscriber).close();

        adapter.endpointDeactivation(endpointFactory, activationSpec);

        verify(spySubscriber).close();
        assertFalse(factories.containsKey(endpointFactory));
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