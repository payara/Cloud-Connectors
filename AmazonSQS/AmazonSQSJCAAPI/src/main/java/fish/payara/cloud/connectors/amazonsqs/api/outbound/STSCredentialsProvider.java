/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.cloud.connectors.amazonsqs.api.outbound;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AWS STS Credentials Provider with caching and thread safety.
 * 
 * This class provides AWS credentials by assuming a role using the AWS Security Token Service (STS).
 * It caches the credentials and ensures thread safety using locks.
 * 
 * @author Gaurav Gupta
 */
public class STSCredentialsProvider implements AWSCredentialsProvider {

    private static final Logger LOGGER = Logger.getLogger(STSCredentialsProvider.class.getName());
    private static final Duration EXPIRATION_THRESHOLD = Duration.ofMinutes(5);
    private final String roleArn;
    private final String roleSessionName;
    private final Regions region;
    private volatile AWSSessionCredentials cachedCredentials;
    private volatile Instant expirationTime;
    private final Lock lock = new ReentrantLock();
    private static final Map<String, STSCredentialsProvider> providerInstances = new HashMap<>();
   
    /**
     * Returns a singleton instance of STSCredentialsProvider for a unique session name.
     * 
     * @param roleArn The ARN of the role to assume.
     * @param roleSessionName The name of the role session.
     * @param region The AWS region.
     * @return The STSCredentialsProvider instance.
     */
    public static STSCredentialsProvider create(String roleArn, String roleSessionName, Regions region) {
        String uniqueSessionKey = roleSessionName + "@" + region.getName();
        return providerInstances.computeIfAbsent(uniqueSessionKey, key -> new STSCredentialsProvider(roleArn, roleSessionName, region));
    }

    private STSCredentialsProvider(String roleArn, String roleSessionName, Regions region) {
        this.roleArn = roleArn;
        this.roleSessionName = roleSessionName;
        this.region = region;
    }

    @Override
    public AWSCredentials getCredentials() {
        if (cachedCredentials != null && !isCredentialsExpired()) {
            LOGGER.fine("Reusing cached AWS session credentials");
            return cachedCredentials;
        } else {
            lock.lock();
            try {
                if (cachedCredentials != null && !isCredentialsExpired()) {
                    LOGGER.fine("Reusing cached AWS session credentials after lock");
                    return cachedCredentials;
                }
                LOGGER.fine("Cached AWS session credentials expired or not present");
                refresh();
                return cachedCredentials;
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean isCredentialsExpired() {
        // Check if the credentials are expired or about to expire
        return expirationTime == null || Instant.now().isAfter(expirationTime.minus(EXPIRATION_THRESHOLD));
    }

    @Override
    public void refresh() {
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard().withRegion(region).build();
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withRoleArn(roleArn)
                .withRoleSessionName(roleSessionName);

        AssumeRoleResult assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
        Credentials stsCredentials = assumeRoleResponse.getCredentials();
        cachedCredentials = new BasicSessionCredentials(
                stsCredentials.getAccessKeyId(),
                stsCredentials.getSecretAccessKey(),
                stsCredentials.getSessionToken()
        );
        expirationTime = stsCredentials.getExpiration().toInstant();
        LOGGER.log(Level.FINE, "Obtained new AWS session credentials - Session Token: {0}, Expiration Time: {1}", new Object[]{stsCredentials.getSessionToken(), stsCredentials.getExpiration()});
    }
}
