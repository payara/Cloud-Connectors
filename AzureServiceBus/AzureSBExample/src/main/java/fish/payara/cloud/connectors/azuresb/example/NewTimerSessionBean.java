package fish.payara.cloud.connectors.azuresb.example;

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
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import fish.payara.cloud.connectors.azuresb.api.AzureSBConnection;
import fish.payara.cloud.connectors.azuresb.api.AzureSBConnectionFactory;
import java.util.LinkedList;
import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.resource.ConnectionFactoryDefinition;
import jakarta.resource.spi.TransactionSupport.TransactionSupportLevel;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@ConnectionFactoryDefinition(name = "java:comp/env/AzureSBConnectionFactory",
        description = "Azure SB Conn Factory",
        interfaceName = "fish.payara.cloud.connectors.azuresb.api.AzureSBConnectionFactory",
        resourceAdapter = "azure-sb-rar-0.9.0-SNAPSHOT",
        minPoolSize = 2, maxPoolSize = 2,
        transactionSupport = TransactionSupportLevel.NoTransaction,
        properties = {"nameSpace=${ENV=nameSpace}",
            "sasKeyName=RootManageSharedAccessKey",
            "sasKey=${ENV=sasKey}",
            "queueName=testq"
        })
@Stateless
public class NewTimerSessionBean {

    @Resource(lookup = "java:comp/env/AzureSBConnectionFactory")
    AzureSBConnectionFactory factory;

    @Schedule(second = "*/1", hour = "*", minute = "*")
    public void myTimer() {
        try (AzureSBConnection connection = factory.getConnection()) {
            LinkedList<IMessage> messages = new LinkedList<>();
            for (int i = 0; i < 10; i++) {
                messages.add(new Message("Hello World " + i));
            }
            connection.sendBatch(messages);
            System.out.println("Sent message");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
