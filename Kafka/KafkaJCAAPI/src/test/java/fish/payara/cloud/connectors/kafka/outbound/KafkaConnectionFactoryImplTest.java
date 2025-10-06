package fish.payara.cloud.connectors.kafka.outbound;

import fish.payara.cloud.connectors.kafka.api.KafkaConnection;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaConnectionFactoryImplTest {

    private ConnectionManager connectionManager;
    private KafkaManagedConnectionFactory managedConnectionFactory;
    private KafkaConnectionFactoryImpl factoryImpl;

    @BeforeEach
    void setUp() {
        connectionManager = mock(ConnectionManager.class);
        managedConnectionFactory = mock(KafkaManagedConnectionFactory.class);
    }

    @Test
    void testGetConnectionWithConnectionManager() throws ResourceException {
        KafkaConnection mockConnection = mock(KafkaConnection.class);
        when(connectionManager.allocateConnection(managedConnectionFactory, null)).thenReturn(mockConnection);

        factoryImpl = new KafkaConnectionFactoryImpl(managedConnectionFactory, connectionManager);
        KafkaConnection connection = factoryImpl.createConnection();

        assertNotNull(connection);
        assertEquals(mockConnection, connection);
        verify(connectionManager).allocateConnection(managedConnectionFactory, null);
    }

    @Test
    void testGetConnectionWithoutConnectionManager() throws ResourceException {
        var managedConnection = mock(KafkaManagedConnection.class);
        when(managedConnectionFactory.createManagedConnection(null, null)).thenReturn(managedConnection);

        factoryImpl = new KafkaConnectionFactoryImpl(managedConnectionFactory, null);
        KafkaConnection connection = factoryImpl.createConnection();

        assertNotNull(connection);
        assertEquals(managedConnection, connection);
        verify(managedConnectionFactory).createManagedConnection(null, null);
    }

    @Test
    void testGetConnectionHandlesResourceException() throws ResourceException {
        when(connectionManager.allocateConnection(managedConnectionFactory, null)).thenThrow(new ResourceException("fail"));

        factoryImpl = new KafkaConnectionFactoryImpl(managedConnectionFactory, connectionManager);
        assertThrows(ResourceException.class, factoryImpl::createConnection);
    }
}