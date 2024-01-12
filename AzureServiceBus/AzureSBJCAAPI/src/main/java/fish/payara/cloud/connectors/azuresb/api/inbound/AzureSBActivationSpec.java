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

import fish.payara.cloud.connectors.azuresb.api.AzureSBListener;
import java.io.PrintWriter;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.Activation;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Activation(messageListeners = AzureSBListener.class)
public class AzureSBActivationSpec implements ActivationSpec {

    @ConfigProperty(defaultValue = "", type = String.class, description = "Namespace")
    private String nameSpace = "";

    @ConfigProperty(defaultValue = "", type = String.class, description = "SAS Key Name")
    private String sasKeyName;

    @ConfigProperty(defaultValue = "", type = String.class, description = "The SAS Key")
    private String sasKey;

    @ConfigProperty(defaultValue = "", type = String.class, description = "The Queue Name")
    private String queueName;

    @ConfigProperty(defaultValue = "10", type = Integer.class, description = "The Prefetch Count")   
    private Integer preFetchCount = 10;
    
    @ConfigProperty(defaultValue = "1", type = Integer.class, description = "The Poll Timeout (in s)")   
    private Integer pollTimeout = 1;
    
    private PrintWriter logWriter;
    
    private ResourceAdapter adapter;

    @ConfigProperty(defaultValue = ".servicebus.windows.net", type = String.class, description = "the base URI that is added to your Service Bus namespace to form the URI to connect to the Service Bus service. To access the default public Azure service, pass \".servicebus.windows.net\"")
    private String serviceBusRootUri = ".servicebus.windows.net";

    @Override
    public void validate() throws InvalidPropertyException {
        if (nameSpace == null || nameSpace.isEmpty()) {
            throw new InvalidPropertyException("NameSpace can not be emtpy");
        }

        if (sasKeyName == null || sasKeyName.isEmpty()) {
            throw new InvalidPropertyException("sasKeyName can not be emtpy");
        }

        if (sasKey == null || sasKey.isEmpty()) {
            throw new InvalidPropertyException("sasKey can not be emtpy");
        }

        if (queueName == null || queueName.isEmpty()) {
            throw new InvalidPropertyException("sasKey can not be emtpy");
        }
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        adapter = ra;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getSasKeyName() {
        return sasKeyName;
    }

    public void setSasKeyName(String sasKeyName) {
        this.sasKeyName = sasKeyName;
    }

    public String getSasKey() {
        return sasKey;
    }

    public void setSasKey(String sasKey) {
        this.sasKey = sasKey;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public Integer getPreFetchCount() {
        return preFetchCount;
    }

    public void setPreFetchCount(Integer preFetchCount) {
        this.preFetchCount = preFetchCount;
    }

    public String getServiceBusRootUri() {
        return serviceBusRootUri;
    }

    public void setServiceBusRootUri(String serviceBusRootUri) {
        this.serviceBusRootUri = serviceBusRootUri;
    }

    public Integer getPollTimeout() {
        return pollTimeout;
    }

    public void setPollTimeout(Integer pollTimeout) {
        this.pollTimeout = pollTimeout;
    }
    
    

}
