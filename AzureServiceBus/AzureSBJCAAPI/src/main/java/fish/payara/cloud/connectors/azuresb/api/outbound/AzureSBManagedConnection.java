/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.cloud.connectors.azuresb.api.outbound;

import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.RetryPolicy;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.microsoft.azure.servicebus.security.SharedAccessSignatureTokenProvider;
import fish.payara.cloud.connectors.azuresb.api.AzureSBConnection;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class AzureSBManagedConnection implements ManagedConnection, AzureSBConnection {

    private final AzureSBManagedConnectionFactory cf;
    private PrintWriter logWriter;
    private final Set<ConnectionEventListener> listeners;
    private final List<AzureSBConnection> connectionHandles = new LinkedList<>();
    private QueueClient sbConnector;
    
    AzureSBManagedConnection(AzureSBManagedConnectionFactory aThis, Subject subject, ConnectionRequestInfo cxRequestInfo) {
        cf = aThis;
        listeners = new HashSet<>();
        SharedAccessSignatureTokenProvider tokenProvider = new SharedAccessSignatureTokenProvider(cf.getSasKeyName(), cf.getSasKey(), 60);
        try {
            sbConnector = new QueueClient(cf.getNameSpace(),cf.getQueueName(),new ClientSettings(tokenProvider, RetryPolicy.getDefault(), Duration.ofSeconds(cf.getTimeOut())),ReceiveMode.RECEIVEANDDELETE);
        } catch (InterruptedException | ServiceBusException ex) {
            Logger.getLogger(AzureSBManagedConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        AzureSBConnection conn = new AzureSBConnectionImpl(this);
        connectionHandles.add(conn);
        return conn;
    }

    @Override
    public void destroy() throws ResourceException {
    }

    @Override
    public void cleanup() throws ResourceException {
        connectionHandles.clear();
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        AzureSBConnectionImpl conn = (AzureSBConnectionImpl)connection;
        conn.setRealConnection(this);
        connectionHandles.add(conn);
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }


    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("Not supported yet."); 
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Not supported yet."); 
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() throws ResourceException {
                return "Azure SB JCA Adapter";
            }

            @Override
            public String getEISProductVersion() throws ResourceException {
                return "1.0.0";
            }

            @Override
            public int getMaxConnections() throws ResourceException {
                return 0;
            }

            @Override
            public String getUserName() throws ResourceException {
                return "anonymous";
            }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    @Override
    public void sendMessage(IMessage message) throws ResourceException {
        try {
            sbConnector.send(message);
        } catch (InterruptedException | ServiceBusException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    public void close() {
        try {
            destroy();
        } catch (ResourceException ex) {
            Logger.getLogger(AzureSBManagedConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    void removeHandle(AzureSBConnectionImpl handle) {
        connectionHandles.remove(handle);
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }

    @Override
    public void sendBatch(Collection<IMessage> messages) throws ResourceException {
        try {
            sbConnector.sendBatch(messages);
        } catch (InterruptedException | ServiceBusException ex) {
            Logger.getLogger(AzureSBManagedConnection.class.getName()).log(Level.SEVERE, "Problem Sending Messages", ex);
            throw new ResourceException(ex);
            
        }
    }
    
}
