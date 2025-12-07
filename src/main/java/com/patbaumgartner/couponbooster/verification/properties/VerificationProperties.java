package com.patbaumgartner.couponbooster.verification.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for account verification.
 * <p>
 * This class holds the configuration for the standalone account verification application,
 * including whether the verification is enabled and the list of accounts to verify.
 *
 * @param enabled whether account verification is enabled
 * @param accounts the list of accounts to verify
 */
@ConfigurationProperties(prefix = "verification")
public record VerificationProperties(boolean enabled, List<AccountConfig> accounts) {

	public VerificationProperties {
		// Default to empty list if null
		if (accounts == null) {
			accounts = List.of();
		}
	}

	/**
	 * Configuration for a single account to verify.
	 *
	 * @param email the email address
	 * @param password the password
	 * @param provider the provider type (migros or coop)
	 */
	public record AccountConfig(String email, String password, String provider) {
	}

}
