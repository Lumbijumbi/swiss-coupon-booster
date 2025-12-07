package com.patbaumgartner.couponbooster.verification.model;

/**
 * Represents account credentials for verification.
 * <p>
 * This record holds the email, password, and provider (Migros or Coop) for an account to
 * be verified.
 *
 * @param email the email address of the account
 * @param password the password of the account
 * @param provider the provider type (MIGROS or COOP)
 */
public record AccountCredentials(String email, String password, AccountProvider provider) {

	/**
	 * Creates a new AccountCredentials instance.
	 * @param email the email address
	 * @param password the password
	 * @param provider the provider type
	 */
	public AccountCredentials {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email cannot be null or blank");
		}
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("Password cannot be null or blank");
		}
		if (provider == null) {
			throw new IllegalArgumentException("Provider cannot be null");
		}
	}

}
