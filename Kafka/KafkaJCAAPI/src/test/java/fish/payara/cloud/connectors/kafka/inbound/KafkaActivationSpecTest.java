package fish.payara.cloud.connectors.kafka.inbound;

import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaActivationSpecTest {

    @Test
    void testSettersAndGetters() {
        KafkaActivationSpec spec = new KafkaActivationSpec();
        spec.setTopics("test-topic");
        spec.setGroupIdConfig("group1");
        spec.setBootstrapServersConfig("localhost:9092");
        spec.setClientId("client1");
        spec.setAutoOffsetReset("earliest");
        assertEquals("test-topic", spec.getTopics());
        assertEquals("group1", spec.getGroupIdConfig());
        assertEquals("localhost:9092", spec.getBootstrapServersConfig());
        assertEquals("client1", spec.getClientId());
        assertEquals("earliest", spec.getAutoOffsetReset());
    }

    @Test
    void testValidateThrowsIfTopicNull() {
        KafkaActivationSpec spec = new KafkaActivationSpec();
        spec.setTopics(null);
        assertThrows(InvalidPropertyException.class, spec::validate);
    }

    @Test
    void testValidateSucceedsIfTopicSet() {
        KafkaActivationSpec spec = new KafkaActivationSpec();
        spec.setTopics("topic");
        spec.setBootstrapServersConfig("localhost:9092");
        spec.setKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        spec.setValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        assertDoesNotThrow(spec::validate);
    }

    @Test
    void testResourceAdapterSetAndGet() throws Exception {
        KafkaActivationSpec spec = new KafkaActivationSpec();
        ResourceAdapter adapter = mock(ResourceAdapter.class);
        spec.setResourceAdapter(adapter);
        assertEquals(adapter, spec.getResourceAdapter());
    }
}