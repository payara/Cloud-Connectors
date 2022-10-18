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
package fish.payara.cloud.connectors.mqtt.api.inbound;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Connector( 
        displayName = "MQTT Resource Adapter",
        vendorName = "Payara Services Limited",
        version = "1.0"
)
public class MQTTResourceAdapter implements ResourceAdapter {

    private static final Logger LOGGER = Logger.getLogger(MQTTResourceAdapter.class.getName());
    private final Map<MessageEndpointFactory, MQTTSubscriber> registeredFactories;
    private BootstrapContext context;

    public MQTTResourceAdapter() {
        registeredFactories = new HashMap<>();
    }
    
    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        context = ctx;
    }

    @Override
    public void stop() {
        for (MQTTSubscriber value : registeredFactories.values()) {
            try {
                value.close();
            } catch (ResourceException ex) {
                Logger.getLogger(MQTTResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        // build a client and associate with the factory
        MQTTSubscriber subscriber = new MQTTSubscriber(endpointFactory, spec, context.getWorkManager());
        registeredFactories.put(endpointFactory, subscriber);
        subscriber.subscribe();
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        MQTTSubscriber subs = registeredFactories.remove(endpointFactory);
        if (subs != null) {
            try {
                subs.close();
            } catch (ResourceException ex) {
                Logger.getLogger(MQTTResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }
    
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
    
}
