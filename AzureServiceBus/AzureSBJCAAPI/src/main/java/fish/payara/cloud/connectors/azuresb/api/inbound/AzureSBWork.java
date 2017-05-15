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

import com.microsoft.windowsazure.services.servicebus.models.BrokeredMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class AzureSBWork implements Work {

    private final MessageEndpointFactory factory;
    private final Method m;
    private final BrokeredMessage bm;
    private MessageEndpoint endpoint;

    public AzureSBWork(MessageEndpointFactory factory, Method m, BrokeredMessage bm) {
        this.factory = factory;
        this.m = m;
        this.bm = bm;
    }

    @Override
    public void release() {
        if (endpoint != null) {
            endpoint.release();
        }
    }

    @Override
    public void run() {
        try {
            endpoint = factory.createEndpoint(null);
            endpoint.beforeDelivery(m);
            if (bm != null) {
                m.invoke(endpoint, bm);
            }
            endpoint.afterDelivery();
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ResourceException ex) {
            Logger.getLogger(AzureSBWork.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (endpoint != null) {
                endpoint.release();
            }
        }
    }
}
