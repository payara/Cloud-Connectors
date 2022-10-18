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
package fish.payara.cloud.connectors.kafka.outbound;

import fish.payara.cloud.connectors.kafka.api.KafkaConnection;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.*;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class KafkaManagedConnection implements ManagedConnection, KafkaConnection {
    
    private KafkaProducer producer;
    private final List<ConnectionEventListener> listeners;
    private final HashSet<KafkaConnectionImpl> connectionHandles;
    private PrintWriter writer;

    KafkaManagedConnection(KafkaProducer producer) {
        listeners = new LinkedList<>();
        connectionHandles = new HashSet<>();
        this.producer = producer;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        KafkaConnectionImpl conn = new KafkaConnectionImpl(this);
        connectionHandles.add(conn);
        return conn;
    }

    @Override
    public void destroy() throws ResourceException {
    }

    @Override
    public void cleanup() throws ResourceException {
        for (KafkaConnectionImpl conn : connectionHandles) {
            conn.setRealConn(null);
        }
        connectionHandles.clear();
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (connection instanceof KafkaConnectionImpl) {
            KafkaConnectionImpl conn = (KafkaConnectionImpl) connection;
            conn.setRealConn(this);
            connectionHandles.add(conn);
        }
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
        throw new NotSupportedException("XA is not supported");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local Transaction Not Supported");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new KafkaConnectionMetadata();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        writer = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return writer;
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord record) {
        return producer.send(record);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord record, Callback callback) {
        return producer.send(record,callback);
    }

    @Override
    public void flush() {
        producer.flush();
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return producer.partitionsFor(topic);
    }

    @Override
    public void close() throws Exception {
        producer.close();
        producer = null;
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() throws ResourceException {
        return producer.metrics();
    }
    
    void remove(KafkaConnectionImpl conn) {
        connectionHandles.remove(conn);
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(conn);
        for (ConnectionEventListener listener : listeners) {
            listener.connectionClosed(event);
        }
    }
    
}
