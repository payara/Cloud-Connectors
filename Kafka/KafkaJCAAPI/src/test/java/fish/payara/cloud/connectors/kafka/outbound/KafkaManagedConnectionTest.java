package fish.payara.cloud.connectors.kafka.outbound;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ManagedConnectionMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import org.apache.kafka.clients.producer.KafkaProducer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaManagedConnectionTest {

    private KafkaManagedConnectionFactory factory;
    private KafkaProducer producer;
    private KafkaManagedConnection managedConnection;

    @BeforeEach
    void setUp() {
        factory = mock(KafkaManagedConnectionFactory.class);
        producer = mock(KafkaProducer.class);
        managedConnection = new KafkaManagedConnection(producer);
    }

    @Test
    void testGetMetaData() throws Exception {
        ManagedConnectionMetaData meta = managedConnection.getMetaData();
        assertNotNull(meta);
        assertEquals("Apache Kafka", meta.getEISProductName());
    }

    @Test
    void testSetAndGetLogWriter() throws Exception {
        PrintWriter pw = new PrintWriter(System.out);
        managedConnection.setLogWriter(pw);
        assertEquals(pw, managedConnection.getLogWriter());
    }

    @Test
    void testCleanupAndDestroy() {
        assertDoesNotThrow(() -> managedConnection.cleanup());
        assertDoesNotThrow(() -> managedConnection.destroy());
    }

    @Test
    void testAddAndRemoveConnectionEventListener() {
        ConnectionEventListener listener = mock(ConnectionEventListener.class);
        managedConnection.addConnectionEventListener(listener);
        managedConnection.removeConnectionEventListener(listener);
        // No exception means success
    }

    @Test
    void testAssociateConnection() {
        Object connection = new Object();
        assertDoesNotThrow(() -> managedConnection.associateConnection(connection));
    }

    @Test
    void testGetConnectionReturnsKafkaConnectionImpl() throws ResourceException {
        Object conn = managedConnection.getConnection(null, null);
        assertNotNull(conn);
        assertTrue(conn instanceof KafkaConnectionImpl);
    }
}