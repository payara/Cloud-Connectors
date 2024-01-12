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

import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.RetryPolicy;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.microsoft.azure.servicebus.security.SharedAccessSignatureTokenProvider;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.WorkException;

/**
 *
 * @author steve
 */
public class AzureSBListener implements IMessageHandler {
    
    private AzureSBActivationSpec activationSpec;
    private BootstrapContext context;
    private MessageEndpointFactory endpointFactory;
    private QueueClient client;
    
    
    public AzureSBListener(AzureSBActivationSpec asbSpec, BootstrapContext context, MessageEndpointFactory endpointFactory) throws ResourceException {
        activationSpec = asbSpec;
        this.context = context;
        this.endpointFactory = endpointFactory;
        SharedAccessSignatureTokenProvider tokenProvider = new SharedAccessSignatureTokenProvider(asbSpec.getSasKeyName(), asbSpec.getSasKey(),60);
        ClientSettings settings = new ClientSettings(tokenProvider, RetryPolicy.getDefault(), Duration.ofSeconds(asbSpec.getPollTimeout()));
        try {
            client = new QueueClient(asbSpec.getNameSpace(), asbSpec.getQueueName(), settings, ReceiveMode.RECEIVEANDDELETE);
            client.setPrefetchCount(activationSpec.getPreFetchCount());
        } catch (InterruptedException | ServiceBusException ex) {
            Logger.getLogger(AzureSBListener.class.getName()).log(Level.SEVERE, "Error creating listener for queue " + activationSpec.getQueueName(), ex);
            throw new ResourceException(ex);
        }
        
    }
    
    public void subscribe() throws ResourceException {
        try {
            client.registerMessageHandler(this);
        } catch (InterruptedException | ServiceBusException ex) {
            Logger.getLogger(AzureSBListener.class.getName()).log(Level.SEVERE, "Error subscribing to queue " + activationSpec.getQueueName(), ex);
            throw new ResourceException(ex);
        }
    }
    
    public void close() {
        client.closeAsync();
    }

    @Override
    public CompletableFuture<Void> onMessageAsync(IMessage im) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            context.getWorkManager().scheduleWork(new AzureSBWork(endpointFactory, im));
        } catch (WorkException ex) {
            Logger.getLogger(AzureSBListener.class.getName()).log(Level.SEVERE, "Exception receoving message", ex);
            result.completeExceptionally(ex);
        }
        result.complete(null);
        return result;
    }

    @Override
    public void notifyException(Throwable thrwbl, ExceptionPhase ep) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
