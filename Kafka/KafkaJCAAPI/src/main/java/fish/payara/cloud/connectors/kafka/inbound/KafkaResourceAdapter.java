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
package fish.payara.cloud.connectors.kafka.inbound;

import java.io.Serializable;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Connector( 
        displayName = "Apache Kafka Resource Adapter",
        vendorName = "Payara Services Limited",
        version = "1.0"
)
public class KafkaResourceAdapter implements ResourceAdapter, Serializable{
    
    private static final Logger LOGGER = Logger.getLogger(KafkaResourceAdapter.class.getName());
    private BootstrapContext context;
    private final Map<MessageEndpointFactory, KafkaPoller> registeredFactories;
    private Timer poller;

    public KafkaResourceAdapter() {
        this.registeredFactories = new ConcurrentHashMap<>();
    }

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        LOGGER.info("Kafka Resource Adapter Started..");
        context = ctx;
        try {
            poller = context.createTimer();
        } catch (UnavailableException ex) {
            LOGGER.log(Level.SEVERE, "Unable to create Poller", ex);
            throw new ResourceAdapterInternalException(ex);
        }
    }

    @Override
    public void stop() {
        LOGGER.info("Kafka Resource Adapter Stopped");
        // go through all the registered factories and stop 
        poller.cancel();
        for (KafkaPoller value : registeredFactories.values()) {
            value.stop();
        }
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        if (spec instanceof KafkaActivationSpec) {
            KafkaActivationSpec kSpec = (KafkaActivationSpec) spec;
            KafkaPoller kTask = new KafkaPoller(kSpec,context,endpointFactory);
            registeredFactories.put(endpointFactory, kTask);
            poller.schedule(kTask, kSpec.getInitialPollDelay(), kSpec.getPollInterval());
        } else {
            LOGGER.warning("Got endpoint activation for an ActivationSpec of unknown class " + spec.getClass().getName());
        } 
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        KafkaPoller kTask = registeredFactories.get(endpointFactory);
        kTask.stop();
        kTask.cancel();
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }

    @Override
    public boolean equals(Object o) {
       return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
