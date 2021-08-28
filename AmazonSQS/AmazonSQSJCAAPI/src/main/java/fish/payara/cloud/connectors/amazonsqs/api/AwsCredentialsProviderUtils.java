package fish.payara.cloud.connectors.amazonsqs.api;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.util.StringUtils;

/**
 * @author Martin Charlesworth
 */
public class AwsCredentialsProviderUtils {

	/**
	 * Creates the appropriate type of {@link AWSCredentialsProvider} depending on the configuration provided.
	 */
	public static AWSCredentialsProvider getProvider(String accessKey,
													 String secretKey,
													 String sessionToken,
													 String profileName) {
		AWSCredentialsProvider awsCredentialsProvider = null;
		if (isValidParam(profileName)) {
			// uses specified credentials profile
			awsCredentialsProvider = new ProfileCredentialsProvider(profileName);

		} else {
			if (isValidParam(accessKey) && isValidParam(secretKey)) {
				if (isValidParam(sessionToken)) {
					// uses temporary session based credentials
					awsCredentialsProvider = new AWSStaticCredentialsProvider(new BasicSessionCredentials(accessKey, secretKey, sessionToken));
				} else {
					// uses basic access key + secret key credentials
					awsCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
				}
			}
		}
		if (awsCredentialsProvider == null) {
			// if neither credentials nor profile are provided, fall back on the default provider chain, which will
			// eventually give us a EC2ContainerCredentialsProviderWrapper, if running in ECS.
			awsCredentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
		}

		return awsCredentialsProvider;
	}

	private static boolean isValidParam(String value) {
		return !StringUtils.isNullOrEmpty(value) && !value.startsWith("${ENV");
	}

}
