package com.patbaumgartner.couponbooster.verification.runner;

import com.patbaumgartner.couponbooster.verification.model.AccountCredentials;
import com.patbaumgartner.couponbooster.verification.model.AccountProvider;
import com.patbaumgartner.couponbooster.verification.model.AccountVerificationResult;
import com.patbaumgartner.couponbooster.verification.properties.VerificationProperties;
import com.patbaumgartner.couponbooster.verification.service.AccountVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Application runner for the standalone account verification application.
 * <p>
 * This runner orchestrates the verification of multiple accounts from a Verein
 * (organization) and their Mitglieder (members). It is conditionally enabled based on the
 * {@code verification.enabled} property.
 */
@Component
@ConditionalOnProperty(value = "verification.enabled", havingValue = "true")
public class AccountVerificationRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AccountVerificationRunner.class);

	private final AccountVerificationService verificationService;

	private final VerificationProperties verificationProperties;

	/**
	 * Constructs a new AccountVerificationRunner.
	 * @param verificationService the service to use for verification
	 * @param verificationProperties the configuration properties
	 */
	public AccountVerificationRunner(AccountVerificationService verificationService,
			VerificationProperties verificationProperties) {
		this.verificationService = verificationService;
		this.verificationProperties = verificationProperties;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting account verification for organization and members");
		log.info("Total accounts to verify: {}", verificationProperties.accounts().size());

		if (verificationProperties.accounts().isEmpty()) {
			log.warn("No accounts configured for verification. Please configure accounts in application.yml");
			return;
		}

		// Convert properties to credentials
		List<AccountCredentials> credentialsList = new ArrayList<>();
		for (var accountConfig : verificationProperties.accounts()) {
			try {
				var provider = parseProvider(accountConfig.provider());
				credentialsList.add(new AccountCredentials(accountConfig.email(), accountConfig.password(), provider));
			}
			catch (IllegalArgumentException e) {
				log.error("Invalid provider '{}' for account {}: must be 'migros' or 'coop'", accountConfig.provider(),
						accountConfig.email());
			}
		}

		if (credentialsList.isEmpty()) {
			log.warn("No valid accounts to verify after parsing configuration");
			return;
		}

		// Verify all accounts
		List<AccountVerificationResult> results = verificationService.verifyAccounts(credentialsList);

		// Print summary
		printVerificationSummary(results);
	}

	private void printVerificationSummary(List<AccountVerificationResult> results) {
		log.info("=".repeat(80));
		log.info("Account Verification Summary");
		log.info("=".repeat(80));

		int successCount = 0;
		int failureCount = 0;

		for (var result : results) {
			if (result.isSuccessful()) {
				successCount++;
				log.info("[SUCCESS] {} - {} - {}ms", result.provider(), result.email(), result.executionDurationMs());
			}
			else {
				failureCount++;
				log.error("[FAILED]  {} - {} - {} ({}ms)", result.provider(), result.email(), result.statusMessage(),
						result.executionDurationMs());
			}
		}

		log.info("=".repeat(80));
		log.info("Total: {} | Successful: {} | Failed: {}", results.size(), successCount, failureCount);
		log.info("=".repeat(80));
	}

	/**
	 * Parses a provider string to an AccountProvider enum. Supports case-insensitive
	 * matching.
	 * @param providerString the provider string (e.g., "migros", "MIGROS", "coop")
	 * @return the corresponding AccountProvider
	 * @throws IllegalArgumentException if the provider string is invalid
	 */
	private AccountProvider parseProvider(String providerString) {
		if (providerString == null || providerString.isBlank()) {
			throw new IllegalArgumentException("Provider cannot be null or blank");
		}

		return switch (providerString.trim().toLowerCase(Locale.ROOT)) {
			case "migros" -> AccountProvider.MIGROS;
			case "coop" -> AccountProvider.COOP;
			default -> throw new IllegalArgumentException(
					"Unknown provider: " + providerString + ". Valid providers are: migros, coop");
		};
	}

}
