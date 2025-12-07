package com.patbaumgartner.couponbooster.verification.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountCredentialsTest {

	@Test
	void shouldCreateAccountCredentialsWithValidData() {
		var credentials = new AccountCredentials("test@example.com", "password123", AccountProvider.MIGROS);

		assertThat(credentials.email()).isEqualTo("test@example.com");
		assertThat(credentials.password()).isEqualTo("password123");
		assertThat(credentials.provider()).isEqualTo(AccountProvider.MIGROS);
	}

	@Test
	void shouldThrowExceptionWhenEmailIsNull() {
		assertThatThrownBy(() -> new AccountCredentials(null, "password", AccountProvider.MIGROS))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Email cannot be null or blank");
	}

	@Test
	void shouldThrowExceptionWhenEmailIsBlank() {
		assertThatThrownBy(() -> new AccountCredentials("", "password", AccountProvider.MIGROS))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Email cannot be null or blank");
	}

	@Test
	void shouldThrowExceptionWhenPasswordIsNull() {
		assertThatThrownBy(() -> new AccountCredentials("test@example.com", null, AccountProvider.MIGROS))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Password cannot be null or blank");
	}

	@Test
	void shouldThrowExceptionWhenPasswordIsBlank() {
		assertThatThrownBy(() -> new AccountCredentials("test@example.com", "", AccountProvider.MIGROS))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Password cannot be null or blank");
	}

	@Test
	void shouldThrowExceptionWhenProviderIsNull() {
		assertThatThrownBy(() -> new AccountCredentials("test@example.com", "password", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Provider cannot be null");
	}

}
