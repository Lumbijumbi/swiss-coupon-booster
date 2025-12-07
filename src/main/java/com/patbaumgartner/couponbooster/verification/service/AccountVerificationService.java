package com.patbaumgartner.couponbooster.verification.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.patbaumgartner.couponbooster.coop.properties.CoopPlaywrightProperties;
import com.patbaumgartner.couponbooster.coop.properties.CoopSelectorsProperties;
import com.patbaumgartner.couponbooster.coop.service.CoopBrowserFactory;
import com.patbaumgartner.couponbooster.migros.properties.MigrosPlaywrightProperties;
import com.patbaumgartner.couponbooster.migros.properties.MigrosSelectorsProperties;
import com.patbaumgartner.couponbooster.migros.service.MigrosBrowserFactory;
import com.patbaumgartner.couponbooster.util.WebAuthnDisabler;
import com.patbaumgartner.couponbooster.verification.model.AccountCredentials;
import com.patbaumgartner.couponbooster.verification.model.AccountProvider;
import com.patbaumgartner.couponbooster.verification.model.AccountVerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Service for verifying multiple accounts from a Verein (organization) and their
 * Mitglieder (members).
 * <p>
 * This service uses the existing authentication logic from Migros and Coop services to
 * verify if accounts are valid (can login successfully).
 */
@Service
public class AccountVerificationService {

	private static final Logger log = LoggerFactory.getLogger(AccountVerificationService.class);

	private final MigrosBrowserFactory migrosBrowserFactory;

	private final CoopBrowserFactory coopBrowserFactory;

	private final MigrosPlaywrightProperties migrosPlaywrightProperties;

	private final CoopPlaywrightProperties coopPlaywrightProperties;

	private final MigrosSelectorsProperties migrosSelectorsProperties;

	private final CoopSelectorsProperties coopSelectorsProperties;

	private final WebAuthnDisabler webAuthnDisabler;

	/**
	 * Constructs a new AccountVerificationService.
	 * @param migrosBrowserFactory factory for creating Migros browsers
	 * @param coopBrowserFactory factory for creating Coop browsers
	 * @param migrosPlaywrightProperties Migros configuration properties
	 * @param coopPlaywrightProperties Coop configuration properties
	 * @param migrosSelectorsProperties Migros element selectors
	 * @param coopSelectorsProperties Coop element selectors
	 * @param webAuthnDisabler utility to disable WebAuthn
	 */
	public AccountVerificationService(MigrosBrowserFactory migrosBrowserFactory, CoopBrowserFactory coopBrowserFactory,
			MigrosPlaywrightProperties migrosPlaywrightProperties, CoopPlaywrightProperties coopPlaywrightProperties,
			MigrosSelectorsProperties migrosSelectorsProperties, CoopSelectorsProperties coopSelectorsProperties,
			WebAuthnDisabler webAuthnDisabler) {
		this.migrosBrowserFactory = Objects.requireNonNull(migrosBrowserFactory, "MigrosBrowserFactory cannot be null");
		this.coopBrowserFactory = Objects.requireNonNull(coopBrowserFactory, "CoopBrowserFactory cannot be null");
		this.migrosPlaywrightProperties = Objects.requireNonNull(migrosPlaywrightProperties,
				"MigrosPlaywrightProperties cannot be null");
		this.coopPlaywrightProperties = Objects.requireNonNull(coopPlaywrightProperties,
				"CoopPlaywrightProperties cannot be null");
		this.migrosSelectorsProperties = Objects.requireNonNull(migrosSelectorsProperties,
				"MigrosSelectorsProperties cannot be null");
		this.coopSelectorsProperties = Objects.requireNonNull(coopSelectorsProperties,
				"CoopSelectorsProperties cannot be null");
		this.webAuthnDisabler = Objects.requireNonNull(webAuthnDisabler, "WebAuthnDisabler cannot be null");
	}

	/**
	 * Verifies a single account by attempting to login.
	 * @param credentials the account credentials to verify
	 * @return the verification result
	 */
	public AccountVerificationResult verifyAccount(AccountCredentials credentials) {
		var startTime = System.currentTimeMillis();
		log.info("Verifying {} account: {}", credentials.provider(), credentials.email());

		try {
			if (credentials.provider() == AccountProvider.MIGROS) {
				return verifyMigrosAccount(credentials, startTime);
			}
			else if (credentials.provider() == AccountProvider.COOP) {
				return verifyCoopAccount(credentials, startTime);
			}
			else {
				throw new IllegalArgumentException("Unsupported provider: " + credentials.provider());
			}
		}
		catch (Exception e) {
			var duration = System.currentTimeMillis() - startTime;
			log.error("Verification failed for {}: {}", credentials.email(), e.getMessage(), e);
			return AccountVerificationResult.failed(credentials.email(), credentials.provider(), e.getMessage(),
					duration);
		}
	}

	/**
	 * Verifies multiple accounts in batch.
	 * @param accountsList the list of account credentials to verify
	 * @return list of verification results
	 */
	public List<AccountVerificationResult> verifyAccounts(List<AccountCredentials> accountsList) {
		log.info("Starting batch verification of {} accounts", accountsList.size());
		return accountsList.stream().map(this::verifyAccount).toList();
	}

	private AccountVerificationResult verifyMigrosAccount(AccountCredentials credentials, long startTime) {
		try (var playwright = Playwright.create()) {
			try (var browser = migrosBrowserFactory.createBrowser(playwright); var context = browser.newContext()) {
				var page = context.newPage();

				// Disable WebAuthn
				page.addInitScript(webAuthnDisabler.getDisableScript());

				// Navigate to login page
				page.navigate(migrosPlaywrightProperties.loginUrl());
				page.waitForLoadState(LoadState.NETWORKIDLE);

				// Handle cookie consent if present
				handleMigrosCookieConsent(page);

				// Enter email
				typeIntoField(page, migrosSelectorsProperties.emailInput(), credentials.email(),
						migrosPlaywrightProperties.typingDelayMs());
				clickElement(page, migrosSelectorsProperties.submitButton());

				// Click password login option
				clickElement(page, migrosSelectorsProperties.passwordLoginLink());

				// Wait for password page
				page.waitForURL(migrosPlaywrightProperties.passwordUrl() + "*",
						new Page.WaitForURLOptions().setTimeout(migrosPlaywrightProperties.timeoutMs()));

				// Disable WebAuthn again on password page
				page.addInitScript(webAuthnDisabler.getDisableScript());

				// Enter password
				typeIntoField(page, migrosSelectorsProperties.passwordInput(), credentials.password(),
						migrosPlaywrightProperties.typingDelayMs());
				clickElement(page, migrosSelectorsProperties.submitButton());

				// Wait for login to complete
				page.waitForLoadState(LoadState.NETWORKIDLE);

				// Check if still on login page (indicates failure)
				String loginDomain = extractDomain(migrosPlaywrightProperties.loginUrl());
				if (page.url().contains(loginDomain)) {
					var duration = System.currentTimeMillis() - startTime;
					return AccountVerificationResult.failed(credentials.email(), credentials.provider(),
							"Authentication failed - still on login page", duration);
				}

				var duration = System.currentTimeMillis() - startTime;
				log.info("Migros account {} verified successfully in {}ms", credentials.email(), duration);
				return AccountVerificationResult.successful(credentials.email(), credentials.provider(), duration);
			}
		}
	}

	private AccountVerificationResult verifyCoopAccount(AccountCredentials credentials, long startTime) {
		try (var playwright = Playwright.create()) {
			try (var contextHandle = coopBrowserFactory.createBrowserContext(playwright,
					createCoopBrowserContextOptions()); var context = contextHandle.get()) {

				var page = context.newPage();

				// Navigate to login page
				page.navigate(coopPlaywrightProperties.loginUrl());
				page.waitForLoadState(LoadState.NETWORKIDLE);

				// Handle cookie consent if present
				handleCoopCookieConsent(page);

				// Check if login is needed
				if (!isLoginLinkVisible(page)) {
					log.info("Coop account appears to be already logged in (no login link found)");
				}
				else {
					// Click login link
					clickElement(page, coopSelectorsProperties.loginLink());

					// Enter credentials
					typeIntoField(page, coopSelectorsProperties.usernameInput(), credentials.email(),
							coopPlaywrightProperties.typingDelayMs());
					typeIntoField(page, coopSelectorsProperties.passwordInput(), credentials.password(),
							coopPlaywrightProperties.typingDelayMs());
					clickElement(page, coopSelectorsProperties.submitButton());

					// Wait for login to complete
					page.waitForLoadState(LoadState.NETWORKIDLE);

					// Check if still showing login form (indicates failure)
					if (isLoginFormVisible(page)) {
						var duration = System.currentTimeMillis() - startTime;
						return AccountVerificationResult.failed(credentials.email(), credentials.provider(),
								"Authentication failed - login form still visible", duration);
					}
				}

				var duration = System.currentTimeMillis() - startTime;
				log.info("Coop account {} verified successfully in {}ms", credentials.email(), duration);
				return AccountVerificationResult.successful(credentials.email(), credentials.provider(), duration);
			}
		}
	}

	private void handleMigrosCookieConsent(Page page) {
		try {
			var acceptButton = page.locator(migrosSelectorsProperties.cookieAcceptButton());
			acceptButton.first().waitFor(new Locator.WaitForOptions().setTimeout(3000));
			if (acceptButton.first().isVisible()) {
				acceptButton.first().click();
				page.waitForLoadState(LoadState.NETWORKIDLE);
			}
		}
		catch (TimeoutError e) {
			log.debug("Cookie consent dialog not found, continuing");
		}
	}

	private void handleCoopCookieConsent(Page page) {
		try {
			var acceptButton = page.locator(coopSelectorsProperties.cookieAcceptButton());
			acceptButton.first().waitFor(new Locator.WaitForOptions().setTimeout(3000));
			if (acceptButton.first().isVisible()) {
				acceptButton.first().click();
				page.waitForLoadState(LoadState.NETWORKIDLE);
			}
		}
		catch (TimeoutError e) {
			log.debug("Cookie consent dialog not found, continuing");
		}
	}

	private boolean isLoginLinkVisible(Page page) {
		try {
			var loginLink = page.locator(coopSelectorsProperties.loginLink()).first();
			loginLink.waitFor(new Locator.WaitForOptions().setTimeout(3000));
			return loginLink.isVisible();
		}
		catch (TimeoutError e) {
			return false;
		}
	}

	private boolean isLoginFormVisible(Page page) {
		try {
			var usernameInput = page.locator(coopSelectorsProperties.usernameInput()).first();
			usernameInput.waitFor(new Locator.WaitForOptions().setTimeout(3000));
			return usernameInput.isVisible();
		}
		catch (TimeoutError e) {
			return false;
		}
	}

	private void typeIntoField(Page page, String selector, String text, int delayMs) {
		var field = page.locator(selector).first();
		field.waitFor(new Locator.WaitForOptions().setTimeout(20000));
		field.clear();
		if (delayMs > 0) {
			field.pressSequentially(text, new Locator.PressSequentiallyOptions().setDelay(delayMs));
		}
		else {
			field.fill(text);
		}
	}

	private void clickElement(Page page, String selector) {
		var element = page.locator(selector).first();
		element.waitFor(new Locator.WaitForOptions().setTimeout(20000));
		element.click();
	}

	private Browser.NewContextOptions createCoopBrowserContextOptions() {
		return new Browser.NewContextOptions().setViewportSize(1920, 1080)
			.setLocale("de-CH")
			.setTimezoneId("Europe/Zurich")
			.setDeviceScaleFactor(1.0)
			.setIsMobile(false)
			.setHasTouch(false)
			.setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT);
	}

	/**
	 * Extracts the domain from a URL.
	 * @param url the URL to extract the domain from
	 * @return the domain portion of the URL
	 */
	private String extractDomain(String url) {
		try {
			// Extract domain from URL (e.g., "https://login.migros.ch/" ->
			// "login.migros.ch")
			return url.replace("https://", "").replace("http://", "").split("/")[0];
		}
		catch (Exception e) {
			log.warn("Failed to extract domain from URL: {}", url, e);
			return url;
		}
	}

}
