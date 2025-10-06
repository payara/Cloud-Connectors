package fish.payara.cloud.connectors.kafka.outbound;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaManagedConnectionFactoryTest {

    private KafkaManagedConnectionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new KafkaManagedConnectionFactory();
        factory.setBootstrapServersConfig("localhost:9092");
        factory.setClientId("test-client");
        factory.setKeySerializer("org.apache.kafka.common.serialization.StringSerializer");
        factory.setValueSerializer("org.apache.kafka.common.serialization.StringSerializer");
    }

    @Test
    void testCreateConnectionFactoryWithManager() throws ResourceException {
        ConnectionManager cm = mock(ConnectionManager.class);
        Object cf = factory.createConnectionFactory(cm);
        assertNotNull(cf);
        assertTrue(cf instanceof KafkaConnectionFactoryImpl);
    }

    @Test
    void testCreateConnectionFactoryWithoutManager() throws ResourceException {
        Object cf = factory.createConnectionFactory();
        assertNotNull(cf);
        assertTrue(cf instanceof KafkaConnectionFactoryImpl);
    }

    @Test
    void testCreateManagedConnection() throws Exception {
        Subject subject = new Subject();
        ConnectionRequestInfo info = mock(ConnectionRequestInfo.class);
        ManagedConnection mc = factory.createManagedConnection(subject, info);
        assertNotNull(mc);
        assertTrue(mc instanceof KafkaManagedConnection);
    }

    @Test
    void testMatchManagedConnectionsWithNonMatchingConnection() throws Exception {
        java.util.Set<ManagedConnection> set = new java.util.HashSet<>();
        ManagedConnection mockConn = mock(ManagedConnection.class);
        set.add(mockConn);
        assertEquals(mockConn, factory.matchManagedConnections(set, null, null));
    }

    @Test
    void testEqualsAndHashCode() {
        KafkaManagedConnectionFactory f1 = new KafkaManagedConnectionFactory();
        f1.setBootstrapServersConfig("localhost:9092");
        f1.setClientId("test-client");
        KafkaManagedConnectionFactory f2 = new KafkaManagedConnectionFactory();
        f2.setBootstrapServersConfig("localhost:9092");
        f2.setClientId("test-client");
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    void testSettersAndGetters() {
        factory.setBootstrapServersConfig("otherhost:9093");
        assertEquals("otherhost:9093", factory.getBootstrapServersConfig());
        factory.setClientId("other-client");
        assertEquals("other-client", factory.getClientId());
    }
}