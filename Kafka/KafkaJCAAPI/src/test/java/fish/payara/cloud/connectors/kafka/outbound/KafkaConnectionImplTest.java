package fish.payara.cloud.connectors.kafka.outbound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaConnectionImplTest {

    private KafkaManagedConnection managedConnection;
    private KafkaConnectionImpl connectionImpl;

    @BeforeEach
    void setUp() {
        managedConnection = mock(KafkaManagedConnection.class);
        connectionImpl = new KafkaConnectionImpl(managedConnection);
    }

    @Test
    void testCloseCallsRemoveHandle() throws Exception {
        doNothing().when(managedConnection).remove(connectionImpl);
        connectionImpl.close();
        verify(managedConnection).remove(connectionImpl);
    }

    @Test
    void testSetRealConnection() {
        KafkaManagedConnection newConn = mock(KafkaManagedConnection.class);
        assertDoesNotThrow(() -> connectionImpl.setRealConn(newConn));
    }

    @Test
    void testToStringAndEquals() {
        assertNotNull(connectionImpl.toString());
        assertNotEquals(connectionImpl, new Object());
    }
}