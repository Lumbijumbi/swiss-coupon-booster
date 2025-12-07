package com.patbaumgartner.couponbooster.verification.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountVerificationResultTest {

	@Test
	void shouldCreateSuccessfulResult() {
		var result = AccountVerificationResult.successful("test@example.com", AccountProvider.MIGROS, 1000L);

		assertThat(result.email()).isEqualTo("test@example.com");
		assertThat(result.provider()).isEqualTo(AccountProvider.MIGROS);
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.statusMessage()).isEqualTo("Account verified successfully");
		assertThat(result.executionDurationMs()).isEqualTo(1000L);
		assertThat(result.verificationTimestamp()).isNotNull();
	}

	@Test
	void shouldCreateFailedResult() {
		var result = AccountVerificationResult.failed("test@example.com", AccountProvider.COOP, "Authentication failed",
				2000L);

		assertThat(result.email()).isEqualTo("test@example.com");
		assertThat(result.provider()).isEqualTo(AccountProvider.COOP);
		assertThat(result.isSuccessful()).isFalse();
		assertThat(result.statusMessage()).isEqualTo("Authentication failed");
		assertThat(result.executionDurationMs()).isEqualTo(2000L);
		assertThat(result.verificationTimestamp()).isNotNull();
	}

}
