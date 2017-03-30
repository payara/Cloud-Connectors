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
package fish.payara.cloud.connectors.kafka;

import fish.payara.cloud.connectors.kafka.api.KafkaListener;
import java.util.Properties;
import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Activation(messageListeners = KafkaListener.class)
public class KafkaActivationSpec implements ActivationSpec {
    
    private final Properties consumerProperties;
    private ResourceAdapter ra;
    private String autoCommitInterval;
    private String bootstrapServersConfig;
    private String clientId;
    private String enableAutoCommit;
    private String groupIdConfig;
    private String valueDeserializer;
    private String keyDeserializer;
    private String topics;
    private String pollInterval = "100";
    private String initialPollDelay = "1000";

    public KafkaActivationSpec() {
        consumerProperties = new Properties();
    }

    @Override
    public void validate() throws InvalidPropertyException {
        // TBD check minimum set.
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.ra = ra;
    }

    public String getAutoCommitInterval() {
        return autoCommitInterval;
    }

    public void setAutoCommitInterval(String autoCommitInterval) {
        this.autoCommitInterval = autoCommitInterval;
        consumerProperties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitInterval);
    }

    public String getBootstrapServersConfig() {
        return bootstrapServersConfig;
    }

    public void setBootstrapServersConfig(String bootstrapServersConfig) {
        this.bootstrapServersConfig = bootstrapServersConfig;
        consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
        consumerProperties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
    }

    public String getEnableAutoCommit() {
        return enableAutoCommit;
    }

    public void setEnableAutoCommit(String enableAutoCommit) {
        this.enableAutoCommit = enableAutoCommit;
        consumerProperties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);        
    }

    public String getGroupIdConfig() {
        return groupIdConfig;
    }

    public void setGroupIdConfig(String groupIdConfig) {
        this.groupIdConfig = groupIdConfig;
        consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupIdConfig);        
    }

    public String getValueDeserializer() {
        return valueDeserializer;
    }

    public void setValueDeserializer(String valueDeserializer) {
        this.valueDeserializer = valueDeserializer;
        consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);        
    }

    public String getKeyDeserializer() {
        return keyDeserializer;
    }

    public void setKeyDeserializer(String keyDeserializer) {
        this.keyDeserializer = keyDeserializer;
        consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);        
    }

    public String getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(String pollInterval) {
        this.pollInterval = pollInterval;
    }
    
    public Properties getConsumerProperties() {
        return consumerProperties;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public String getInitialPollDelay() {
        return initialPollDelay;
    }

    public void setInitialPollDelay(String initialPollDelay) {
        this.initialPollDelay = initialPollDelay;
    }
    
    
    
}
