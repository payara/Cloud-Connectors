/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2024 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.StringUtils;
import fish.payara.cloud.connectors.amazonsqs.api.AmazonSQSListener;
import fish.payara.cloud.connectors.amazonsqs.api.outbound.STSCredentialsProvider;

import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;


/**
 * Activation Specification for Amazon SQS
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Activation(messageListeners = AmazonSQSListener.class)
public class AmazonSQSActivationSpec implements ActivationSpec, AWSCredentialsProvider {
    
    ResourceAdapter adapter;
    
    private String awsAccessKeyId;
    private String awsSecretKey;
    private String queueURL;
    private String region;
    private Integer maxMessages = 10;
    private Integer visibilityTimeout = 30;
    private Integer initialPollDelay = 1;
    private Integer pollInterval = 1000;
    private String messageAttributeNames = "All";
    private String attributeNames = "All";
    private String profileName;
    private String roleArn;
    private String roleSessionName;
    private String s3BucketName;
    private Integer s3SizeThreshold = 0;
    private String s3KeyPrefix;
    private Boolean s3FetchMessage = true;

    @Override
    public void validate() throws InvalidPropertyException {
        if (StringUtils.isNullOrEmpty(region)) {
            throw new InvalidPropertyException("region must be specified");
        }
        
        if (StringUtils.isNullOrEmpty(queueURL)) {
            throw new InvalidPropertyException("queueURL must be specified");
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
    
    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }
    
    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.awsAccessKeyId = awsAccessKeyId;
    }
    
    public String getAwsSecretKey() {
        return awsSecretKey;
    }
    
    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }
    
    public String getQueueURL() {
        return queueURL;
    }
    
    public void setQueueURL(String queueURL) {
        this.queueURL = queueURL;
    }
    
    public Integer getMaxMessages() {
        return maxMessages;
    }
    
    public void setMaxMessages(Integer maxMessages) {
        this.maxMessages = maxMessages;
    }
    
    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }
    
    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }
    
    public Integer getInitialPollDelay() {
        return initialPollDelay;
    }
    
    public void setInitialPollDelay(Integer initialPollDelay) {
        this.initialPollDelay = initialPollDelay;
    }
    
    public Integer getPollInterval() {
        return pollInterval;
    }
    
    public void setPollInterval(Integer pollInterval) {
        this.pollInterval = pollInterval;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getMessageAttributeNames() {
        return messageAttributeNames;
    }
    
    public void setMessageAttributeNames(String messageAttributeNames) {
        this.messageAttributeNames = messageAttributeNames;
    }
    
    public String getAttributeNames() {
        return attributeNames;
    }
    
    public void setAttributeNames(String attributeNames) {
        this.attributeNames = attributeNames;
    }
    
    public String getProfileName() {
        return profileName;
    }
    
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getRoleSessionName() {
        return roleSessionName;
    }

    public void setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

    public void setS3BucketName(String s3BucketName) {
        this.s3BucketName = s3BucketName;
    }

    public Integer getS3SizeThreshold() {
        return s3SizeThreshold;
    }

    public void setS3SizeThreshold(Integer s3SizeThreshold) {
        this.s3SizeThreshold = s3SizeThreshold;
    }

    public String getS3KeyPrefix() {
        return s3KeyPrefix;
    }

    public void setS3KeyPrefix(String s3KeyPrefix) {
        this.s3KeyPrefix = s3KeyPrefix;
    }

    public Boolean getS3FetchMessage() {
        return s3FetchMessage;
    }

    public void setS3FetchMessage(Boolean s3FetchMessage) {
        this.s3FetchMessage = s3FetchMessage;
    }

    @Override
    public AWSCredentials getCredentials() {

        // Return Credentials based on what is present, roleArn taking priority.
        if (!StringUtils.isNullOrEmpty(getRoleArn())) {
            return STSCredentialsProvider.create(getRoleArn(), getRoleSessionName(), Regions.fromName(getRegion())).getCredentials();
        } else if (StringUtils.isNullOrEmpty(getProfileName())) {

            if (!StringUtils.isNullOrEmpty(awsAccessKeyId) && !StringUtils.isNullOrEmpty(awsSecretKey)) {
                return new AWSCredentials() {
                    @Override
                    public String getAWSAccessKeyId() {
                        return awsAccessKeyId;
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return awsSecretKey;
                    }
                };
            } else {
                return DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
            }

        } else {
            return new ProfileCredentialsProvider(getProfileName()).getCredentials();
        }
    }
    
    @Override
    public void refresh() {
        
    }
    
}
