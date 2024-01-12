/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2022 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.cloud.connectors.amazonsqs.api.inbound;

import java.io.Serializable;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * @author Steve Millidge (Payara Foundation)
 */
@Connector(
        displayName = "Amazon SQS Resource Adapter",
        vendorName = "Payara Services Limited",
        version = "1.0"
)
public class AmazonSQSResourceAdapter implements ResourceAdapter, Serializable {

    private static final Logger LOGGER = Logger.getLogger(AmazonSQSResourceAdapter.class.getName());
    private final Map<MessageEndpointFactory, SQSPoller> registeredFactories;
    private final Map<MessageEndpointFactory, Timer> registeredTimers;
    private BootstrapContext context;

    public AmazonSQSResourceAdapter() {
        registeredFactories = new ConcurrentHashMap<>();
        registeredTimers = new ConcurrentHashMap<>();
    }

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        LOGGER.info("Amazon SQS Resource Adapter Started..");
        context = ctx;
    }

    @Override
    public void stop() {
        LOGGER.info("Amazon SQS Resource Adapter Stopped");
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        if (spec instanceof AmazonSQSActivationSpec) {
            AmazonSQSActivationSpec sqsSpec = (AmazonSQSActivationSpec) spec;
            SQSPoller sqsTask = new SQSPoller(sqsSpec, context, endpointFactory);
            registeredFactories.put(endpointFactory, sqsTask);

            Timer timer = createTimer();
            registeredTimers.put(endpointFactory, timer);
            timer.schedule(sqsTask, sqsSpec.getInitialPollDelay(), sqsSpec.getPollInterval());

        } else {
            LOGGER.log(Level.WARNING, "Got endpoint activation for an ActivationSpec of unknown class {0}", spec.getClass().getName());
        }
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        SQSPoller sqsTask = registeredFactories.get(endpointFactory);
        sqsTask.cancel();

        Timer timer = registeredTimers.get(endpointFactory);
        timer.cancel();
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }

    private Timer createTimer() throws ResourceAdapterInternalException {
        try {
            return context.createTimer();

        } catch (UnavailableException ex) {
            LOGGER.log(Level.SEVERE, "Unable to create Poller", ex);
            throw new ResourceAdapterInternalException(ex);
        }
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
