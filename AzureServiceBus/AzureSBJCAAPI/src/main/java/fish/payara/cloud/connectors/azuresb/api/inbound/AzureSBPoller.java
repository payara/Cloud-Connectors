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
package fish.payara.cloud.connectors.azuresb.api.inbound;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import com.microsoft.windowsazure.services.servicebus.models.BrokeredMessage;
import com.microsoft.windowsazure.services.servicebus.models.ReceiveMessageOptions;
import com.microsoft.windowsazure.services.servicebus.models.ReceiveMessageResult;
import fish.payara.cloud.connectors.azuresb.api.OnAzureSBMessage;
import java.lang.reflect.Method;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkException;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
class AzureSBPoller extends TimerTask {

    private AzureSBActivationSpec spec;
    private BootstrapContext ctx;
    private MessageEndpointFactory factory;
    private ServiceBusContract sbConnector;

    AzureSBPoller(AzureSBActivationSpec sqsSpec, BootstrapContext context, MessageEndpointFactory endpointFactory) {
        spec = sqsSpec;
        ctx = context;
        factory = endpointFactory;
        Configuration configureWithSASAuthentication = ServiceBusConfiguration.configureWithSASAuthentication(sqsSpec.getNameSpace(), sqsSpec.getSasKeyName(), sqsSpec.getSasKey(), sqsSpec.getServiceBusRootUri());
        sbConnector = ServiceBusService.create(configureWithSASAuthentication);
    }

    void stop() {

    }

    @Override
    public void run() {
        ReceiveMessageOptions rmo = new ReceiveMessageOptions();
        rmo.setReceiveAndDelete();
        rmo.setTimeout(spec.getPollTimeout());
        try {
            ReceiveMessageResult receiveMessage = sbConnector.receiveMessage(spec.getQueueName(), rmo);
            BrokeredMessage bm = receiveMessage.getValue();
            if (bm != null) {
                Class<?> mdbClass = factory.getEndpointClass();
                for (Method m : mdbClass.getMethods()) {
                    if (m.isAnnotationPresent(OnAzureSBMessage.class) && m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(BrokeredMessage.class)) {
                        try {
                            ctx.getWorkManager().scheduleWork(new AzureSBWork(factory, m, bm));
                        } catch (WorkException ex) {
                            Logger.getLogger(AzureSBPoller.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        } catch (ServiceException ex) {
            Logger.getLogger(AzureSBPoller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
