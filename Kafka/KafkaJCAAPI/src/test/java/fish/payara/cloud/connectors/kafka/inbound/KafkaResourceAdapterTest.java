package fish.payara.cloud.connectors.kafka.inbound;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaResourceAdapterTest {

    private KafkaResourceAdapter adapter;
    private BootstrapContext bootstrapContext;
    private MessageEndpointFactory endpointFactory;
    private ActivationSpec activationSpec;

    @BeforeEach
    void setUp() throws ResourceAdapterInternalException {
        adapter = new KafkaResourceAdapter();
        bootstrapContext = mock(BootstrapContext.class);
        endpointFactory = mock(MessageEndpointFactory.class);
        activationSpec = mock(ActivationSpec.class);

        when(endpointFactory.getEndpointClass()).thenAnswer(invocation -> Object.class);
        when(bootstrapContext.getWorkManager()).thenReturn(mock(jakarta.resource.spi.work.WorkManager.class));
        adapter.start(bootstrapContext);

    }

    @Test
    void testStartSetsContext() throws Exception {
        adapter.start(bootstrapContext);
        var contextField = KafkaResourceAdapter.class.getDeclaredField("context");
        contextField.setAccessible(true);
        assertEquals(bootstrapContext, contextField.get(adapter));
    }

    @Test
    void testStopDoesNotThrow() {
        assertDoesNotThrow(() -> adapter.stop());
    }

    @Test
    void testEndpointActivationAndDeactivation() {
        KafkaActivationSpec spec = new KafkaActivationSpec();
        spec.setTopics("topic");
        spec.setBootstrapServersConfig("localhost:9092");
        spec.setKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        spec.setValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        spec.setGroupIdConfig("test-group");
        assertDoesNotThrow(() -> adapter.endpointActivation(endpointFactory, spec));
        assertDoesNotThrow(() -> adapter.endpointDeactivation(endpointFactory, spec));
    }

    @Test
    void testGetXAResourcesReturnsNull() throws Exception {
        XAResource[] result = adapter.getXAResources(new ActivationSpec[0]);
        assertNull(result);
    }

    @Test
    void testEqualsAndHashCode() {
        KafkaResourceAdapter adapter2 = new KafkaResourceAdapter();
        assertEquals(adapter, adapter);
        assertNotEquals(adapter, adapter2); // default equals uses reference
        assertEquals(adapter.hashCode(), adapter.hashCode());
    }
}