package fish.payara.cloud.connectors.mqtt.api.inbound;

import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.ResourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MQTTActivationSpecTest {

    @Test
    void testDefaultValues() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        assertEquals("tcp://localhost:1883", spec.getServerURIs());
        assertFalse(spec.getCleanSession());
        assertTrue(spec.getAutomaticReconnect());
        assertFalse(spec.getFilePersistance());
        assertEquals(".", spec.getPersistenceDirectory());
        assertEquals(30, spec.getConnectionTimeout());
        assertEquals(10, spec.getMaxInflight());
        assertEquals(60, spec.getKeepAliveInterval());
        assertNull(spec.getUserName());
        assertNull(spec.getPassword());
        assertNull(spec.getTopicFilter());
        assertEquals(0, spec.getQos());
        assertNull(spec.getClientId());
    }

    @Test
    void testSettersAndGetters() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        spec.setServerURIs("tcp://test:1883");
        spec.setCleanSession(true);
        spec.setAutomaticReconnect(false);
        spec.setFilePersistance(true);
        spec.setPersistenceDirectory("/tmp");
        spec.setConnectionTimeout(15);
        spec.setMaxInflight(20);
        spec.setKeepAliveInterval(120);
        spec.setUserName("user");
        spec.setPassword("pass");
        spec.setTopicFilter("topic/test");
        spec.setQos(2);
        spec.setClientId("client123");

        assertEquals("tcp://test:1883", spec.getServerURIs());
        assertTrue(spec.getCleanSession());
        assertFalse(spec.getAutomaticReconnect());
        assertTrue(spec.getFilePersistance());
        assertEquals("/tmp", spec.getPersistenceDirectory());
        assertEquals(15, spec.getConnectionTimeout());
        assertEquals(20, spec.getMaxInflight());
        assertEquals(120, spec.getKeepAliveInterval());
        assertEquals("user", spec.getUserName());
        assertEquals("pass", spec.getPassword());
        assertEquals("topic/test", spec.getTopicFilter());
        assertEquals(2, spec.getQos());
        assertEquals("client123", spec.getClientId());
    }

    @Test
    void testValidateThrowsIfTopicFilterNull() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        spec.setTopicFilter(null);
        assertThrows(InvalidPropertyException.class, spec::validate);
    }

    @Test
    void testValidateSucceedsIfTopicFilterSet() {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        spec.setTopicFilter("topic/test");
        assertDoesNotThrow(spec::validate);
    }

    @Test
    void testResourceAdapterSetAndGet() throws ResourceException {
        MQTTActivationSpec spec = new MQTTActivationSpec();
        ResourceAdapter adapter = mock(ResourceAdapter.class);
        spec.setResourceAdapter(adapter);
        assertEquals(adapter, spec.getResourceAdapter());
    }
}