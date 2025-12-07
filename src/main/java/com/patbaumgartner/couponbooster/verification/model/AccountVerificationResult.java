package com.patbaumgartner.couponbooster.verification.model;

import java.time.Instant;

/**
 * Represents the result of an account verification attempt.
 * <p>
 * This record contains information about whether the verification was successful, the
 * email that was verified, the provider, any error message, and timing information.
 *
 * @param email the email address that was verified
 * @param provider the account provider (MIGROS or COOP)
 * @param isSuccessful whether the verification was successful
 * @param statusMessage the status message (error message if failed)
 * @param verificationTimestamp the timestamp when verification was completed
 * @param executionDurationMs the duration of the verification in milliseconds
 */
public record AccountVerificationResult(String email, AccountProvider provider, boolean isSuccessful,
		String statusMessage, Instant verificationTimestamp, long executionDurationMs) {

	/**
	 * Creates a successful verification result.
	 * @param email the verified email
	 * @param provider the account provider
	 * @param executionDurationMs the execution duration
	 * @return a successful verification result
	 */
	public static AccountVerificationResult successful(String email, AccountProvider provider,
			long executionDurationMs) {
		return new AccountVerificationResult(email, provider, true, "Account verified successfully", Instant.now(),
				executionDurationMs);
	}

	/**
	 * Creates a failed verification result.
	 * @param email the email that failed verification
	 * @param provider the account provider
	 * @param errorMessage the error message
	 * @param executionDurationMs the execution duration
	 * @return a failed verification result
	 */
	public static AccountVerificationResult failed(String email, AccountProvider provider, String errorMessage,
			long executionDurationMs) {
		return new AccountVerificationResult(email, provider, false, errorMessage, Instant.now(), executionDurationMs);
	}

}
