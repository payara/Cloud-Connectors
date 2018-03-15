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

import fish.payara.cloud.connectors.kafka.api.KafkaConnectionFactory;
import fish.payara.cloud.connectors.kafka.api.KafkaConnection;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Properties;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import fish.payara.cloud.connectors.kafka.tools.AdditionalPropertiesParser;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@ConnectionDefinition( connection = KafkaConnection.class,
        connectionFactory = KafkaConnectionFactory.class,
        connectionFactoryImpl = KafkaConnectionFactoryImpl.class,
        connectionImpl = KafkaConnectionImpl.class
)
public class KafkaManagedConnectionFactory implements ManagedConnectionFactory, Serializable {


	private static final long serialVersionUID = 6526129596378966626L;
	private final Properties producerProperties;
    private AdditionalPropertiesParser additionalPropertiesParser;

    @ConfigProperty(defaultValue = "localhost:9092", description = "Kafka Servers to Connect to", type = String.class)
    private String bootstrapServersConfig;

    @ConfigProperty(defaultValue = "KafkaJCAClient", description = "Client ID of the Producer", type = String.class)
    private String clientId;

    @ConfigProperty(defaultValue = "org.apache.kafka.common.serialization.StringSerializer", type = String.class, description = "Serializer class for key")
    private String valueSerializer;

    @ConfigProperty(defaultValue = "org.apache.kafka.common.serialization.StringSerializer", type = String.class, description = "Serializer class for value")
    private String keySerializer;

    @ConfigProperty( type = Long.class, defaultValue = "33554432", description = "The total bytes the producer can use to buffer messages")
    private Long bufferMemory;

    @ConfigProperty(type = String.class, description = "The number of acks the producer requires", defaultValue = "1")
    private String acks;

    @ConfigProperty(type = Integer.class, description = "The number of retries if there is a transient error", defaultValue = "0")
    private Integer retries;

    @ConfigProperty(type = Long.class, description = "The producer will attempt to batch records together into fewer requests whenever multiple records are being sent to the same partition", defaultValue = "16384")
    private Long batchSize;

    @ConfigProperty(type = Long.class, defaultValue = "0", description = "The producer groups together any records that arrive in between request transmissions into a single batched request. ")
    private Long lingerMS;

    @ConfigProperty(type = Long.class, defaultValue = "60000", description = "How long can send block ")
    private Long maxBlockMS;

    @ConfigProperty(type = Long.class, defaultValue = "1048576", description = "Maximum size of request (bytes)")
    private Long maxRequestSize;

    @ConfigProperty(type = Integer.class, defaultValue = "32768", description = "Receive Buffer (bytes)")
    private Integer receiveBufferBytes;

    @ConfigProperty(type = Integer.class, defaultValue = "30000", description = "Request Timeout (ms)")
    private Integer requestTimeout;

    @ConfigProperty(type = String.class, description = "Compression type of data sent", defaultValue = "none")
    private String compression;

    @ConfigProperty(type = Long.class, description = "Close Idle Kafka Connections", defaultValue = "540000")
    private Long connectionsMaxIdle;

    @ConfigProperty(type = Integer.class, defaultValue = "5", description = "Maximum unacknowledged requests to send before blocking")
    private Integer maxInflightConnections;

    @ConfigProperty(type = Long.class, description = "Period of time before a refresh of Metadata (ms)", defaultValue = "300000")
    private Long metadataMaxAge;

    @ConfigProperty(type = Long.class, description = "The amount of time to wait before attempting a retry (ms)", defaultValue = "100")
    private Long retryBackoff;

    @ConfigProperty(type = Long.class, description = "The amount of time to wait before attempting a reconnection (ms)", defaultValue = "100")
    private Long reconnectBackoff;

    @ConfigProperty(type = String.class, description = "Additional properties to be passed to the KafkaConnection.")
    private String additionalProperties;

    transient private PrintWriter writer;
    
    transient private KafkaProducer<?, ?> producer;


    public KafkaManagedConnectionFactory() {
        producerProperties = new Properties();
    }

    public String getBootstrapServersConfig() {
        return bootstrapServersConfig;
    }

    public void setBootstrapServersConfig(String bootstrapServersConfig) {
        this.bootstrapServersConfig = bootstrapServersConfig;
        producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
        producerProperties.setProperty(ProducerConfig.CLIENT_ID_CONFIG, clientId);
    }

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueDeserializer) {
        this.valueSerializer = valueDeserializer;
        producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueDeserializer);
    }

    public String getKeySerializer() {
        return keySerializer;
    }

    public void setKeySerializer(String keyDeserializer) {
        this.keySerializer = keyDeserializer;
        producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keyDeserializer);
    }

    public Long getBufferMemory() {
        return bufferMemory;
    }

    public void setBufferMemory(Long bufferMemory) {
        this.bufferMemory = bufferMemory;
        producerProperties.setProperty(ProducerConfig.BUFFER_MEMORY_CONFIG, Long.toString(bufferMemory));
    }

    public String getAcks() {
        return acks;
    }

    public void setAcks(String acks) {
        this.acks = acks;
        producerProperties.setProperty(ProducerConfig.ACKS_CONFIG, acks);
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
        producerProperties.setProperty(ProducerConfig.RETRIES_CONFIG, Long.toString(retries));
    }

    public Long getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
        producerProperties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Long.toString(batchSize));
    }

    public Long getLingerMS() {
        return lingerMS;
    }

    public void setLingerMS(Long lingerMS) {
        this.lingerMS = lingerMS;
        producerProperties.setProperty(ProducerConfig.LINGER_MS_CONFIG, Long.toString(lingerMS));
    }

    public Properties getProducerProperties() {
        return producerProperties;
    }

    public Long getMaxBlockMS() {
        return maxBlockMS;
    }

    public void setMaxBlockMS(Long maxBlockMS) {
        this.maxBlockMS = maxBlockMS;
        producerProperties.setProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, Long.toString(maxBlockMS));
    }

    public Long getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(Long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
        producerProperties.setProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, Long.toString(maxRequestSize));
    }

    public Integer getReceiveBufferBytes() {
        return receiveBufferBytes;
    }

    public void setReceiveBufferBytes(Integer receiveBufferBytes) {
        this.receiveBufferBytes = receiveBufferBytes;
        producerProperties.setProperty(ProducerConfig.RECEIVE_BUFFER_CONFIG, Integer.toString(receiveBufferBytes));
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        producerProperties.setProperty(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, Integer.toString(requestTimeout));
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
        producerProperties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, compression);
    }

    public Long getConnectionsMaxIdle() {
        return connectionsMaxIdle;
    }

    public void setConnectionsMaxIdle(Long maxIdle) {
        this.connectionsMaxIdle = maxIdle;
        producerProperties.setProperty(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, Long.toString(maxIdle));
    }

    public Integer getMaxInflightConnections() {
        return maxInflightConnections;
    }

    public void setMaxInflightConnections(Integer maxInflightConnections) {
        this.maxInflightConnections = maxInflightConnections;
        producerProperties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, Integer.toString(maxInflightConnections));
    }

    public Long getMetadataMaxAge() {
        return metadataMaxAge;
    }

    public void setMetadataMaxAge(Long metadataMaxAge) {
        this.metadataMaxAge = metadataMaxAge;
        producerProperties.setProperty(ProducerConfig.METADATA_MAX_AGE_CONFIG, Long.toString(metadataMaxAge));
    }

    public Long getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Long retryBackoff) {
        this.retryBackoff = retryBackoff;
        producerProperties.setProperty(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, Long.toString(retryBackoff));
    }

    public Long getReconnectBackoff() {
        return reconnectBackoff;
    }

    public void setReconnectBackoff(Long reconnectBackoff) {
        this.reconnectBackoff = reconnectBackoff;
        producerProperties.setProperty(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, Long.toString(reconnectBackoff));
    }

    public String getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(String additionalProperties) {
        this.additionalProperties = additionalProperties;
        this.additionalPropertiesParser = new AdditionalPropertiesParser(additionalProperties);
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        Properties properties =
                additionalPropertiesParser == null
                        ? producerProperties
                        : AdditionalPropertiesParser.merge(producerProperties,  additionalPropertiesParser.parse());
        if (producer == null) {
            producer = new KafkaProducer<Object, Object>(properties);
        }
        return new KafkaConnectionFactoryImpl(this,cxManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        Properties properties =
                additionalPropertiesParser == null
                        ? producerProperties
                        : AdditionalPropertiesParser.merge(producerProperties,  additionalPropertiesParser.parse());
        if (producer == null) {
            producer = new KafkaProducer<Object, Object>(properties);
        }
        return new KafkaConnectionFactoryImpl(this, null);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        @SuppressWarnings("unused")
		Properties properties =
                additionalPropertiesParser == null
                        ? producerProperties
                        : AdditionalPropertiesParser.merge(producerProperties,  additionalPropertiesParser.parse());
        return new KafkaManagedConnection(producer);
    }

    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return (ManagedConnection) connectionSet.toArray()[0];
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        writer = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return writer;
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((acks == null) ? 0 : acks.hashCode());
		result = prime * result + ((additionalProperties == null) ? 0 : additionalProperties.hashCode());
		result = prime * result + ((additionalPropertiesParser == null) ? 0 : additionalPropertiesParser.hashCode());
		result = prime * result + ((batchSize == null) ? 0 : batchSize.hashCode());
		result = prime * result + ((bootstrapServersConfig == null) ? 0 : bootstrapServersConfig.hashCode());
		result = prime * result + ((bufferMemory == null) ? 0 : bufferMemory.hashCode());
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((compression == null) ? 0 : compression.hashCode());
		result = prime * result + ((connectionsMaxIdle == null) ? 0 : connectionsMaxIdle.hashCode());
		result = prime * result + ((keySerializer == null) ? 0 : keySerializer.hashCode());
		result = prime * result + ((lingerMS == null) ? 0 : lingerMS.hashCode());
		result = prime * result + ((maxBlockMS == null) ? 0 : maxBlockMS.hashCode());
		result = prime * result + ((maxInflightConnections == null) ? 0 : maxInflightConnections.hashCode());
		result = prime * result + ((maxRequestSize == null) ? 0 : maxRequestSize.hashCode());
		result = prime * result + ((metadataMaxAge == null) ? 0 : metadataMaxAge.hashCode());
		result = prime * result + ((producerProperties == null) ? 0 : producerProperties.hashCode());
		result = prime * result + ((receiveBufferBytes == null) ? 0 : receiveBufferBytes.hashCode());
		result = prime * result + ((reconnectBackoff == null) ? 0 : reconnectBackoff.hashCode());
		result = prime * result + ((requestTimeout == null) ? 0 : requestTimeout.hashCode());
		result = prime * result + ((retries == null) ? 0 : retries.hashCode());
		result = prime * result + ((retryBackoff == null) ? 0 : retryBackoff.hashCode());
		result = prime * result + ((valueSerializer == null) ? 0 : valueSerializer.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof KafkaManagedConnectionFactory)) {
			return false;
		}
		KafkaManagedConnectionFactory other = (KafkaManagedConnectionFactory) obj;
		if (acks == null) {
			if (other.acks != null) {
				return false;
			}
		} else if (!acks.equals(other.acks)) {
			return false;
		}
		if (additionalProperties == null) {
			if (other.additionalProperties != null) {
				return false;
			}
		} else if (!additionalProperties.equals(other.additionalProperties)) {
			return false;
		}
		if (additionalPropertiesParser == null) {
			if (other.additionalPropertiesParser != null) {
				return false;
			}
		} else if (!additionalPropertiesParser.equals(other.additionalPropertiesParser)) {
			return false;
		}
		if (batchSize == null) {
			if (other.batchSize != null) {
				return false;
			}
		} else if (!batchSize.equals(other.batchSize)) {
			return false;
		}
		if (bootstrapServersConfig == null) {
			if (other.bootstrapServersConfig != null) {
				return false;
			}
		} else if (!bootstrapServersConfig.equals(other.bootstrapServersConfig)) {
			return false;
		}
		if (bufferMemory == null) {
			if (other.bufferMemory != null) {
				return false;
			}
		} else if (!bufferMemory.equals(other.bufferMemory)) {
			return false;
		}
		if (clientId == null) {
			if (other.clientId != null) {
				return false;
			}
		} else if (!clientId.equals(other.clientId)) {
			return false;
		}
		if (compression == null) {
			if (other.compression != null) {
				return false;
			}
		} else if (!compression.equals(other.compression)) {
			return false;
		}
		if (connectionsMaxIdle == null) {
			if (other.connectionsMaxIdle != null) {
				return false;
			}
		} else if (!connectionsMaxIdle.equals(other.connectionsMaxIdle)) {
			return false;
		}
		if (keySerializer == null) {
			if (other.keySerializer != null) {
				return false;
			}
		} else if (!keySerializer.equals(other.keySerializer)) {
			return false;
		}
		if (lingerMS == null) {
			if (other.lingerMS != null) {
				return false;
			}
		} else if (!lingerMS.equals(other.lingerMS)) {
			return false;
		}
		if (maxBlockMS == null) {
			if (other.maxBlockMS != null) {
				return false;
			}
		} else if (!maxBlockMS.equals(other.maxBlockMS)) {
			return false;
		}
		if (maxInflightConnections == null) {
			if (other.maxInflightConnections != null) {
				return false;
			}
		} else if (!maxInflightConnections.equals(other.maxInflightConnections)) {
			return false;
		}
		if (maxRequestSize == null) {
			if (other.maxRequestSize != null) {
				return false;
			}
		} else if (!maxRequestSize.equals(other.maxRequestSize)) {
			return false;
		}
		if (metadataMaxAge == null) {
			if (other.metadataMaxAge != null) {
				return false;
			}
		} else if (!metadataMaxAge.equals(other.metadataMaxAge)) {
			return false;
		}
		if (producerProperties == null) {
			if (other.producerProperties != null) {
				return false;
			}
		} else if (!producerProperties.equals(other.producerProperties)) {
			return false;
		}
		if (receiveBufferBytes == null) {
			if (other.receiveBufferBytes != null) {
				return false;
			}
		} else if (!receiveBufferBytes.equals(other.receiveBufferBytes)) {
			return false;
		}
		if (reconnectBackoff == null) {
			if (other.reconnectBackoff != null) {
				return false;
			}
		} else if (!reconnectBackoff.equals(other.reconnectBackoff)) {
			return false;
		}
		if (requestTimeout == null) {
			if (other.requestTimeout != null) {
				return false;
			}
		} else if (!requestTimeout.equals(other.requestTimeout)) {
			return false;
		}
		if (retries == null) {
			if (other.retries != null) {
				return false;
			}
		} else if (!retries.equals(other.retries)) {
			return false;
		}
		if (retryBackoff == null) {
			if (other.retryBackoff != null) {
				return false;
			}
		} else if (!retryBackoff.equals(other.retryBackoff)) {
			return false;
		}
		if (valueSerializer == null) {
			if (other.valueSerializer != null) {
				return false;
			}
		} else if (!valueSerializer.equals(other.valueSerializer)) {
			return false;
		}
		return true;
	}
}
