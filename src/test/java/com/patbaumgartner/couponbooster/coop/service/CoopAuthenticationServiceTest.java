package com.patbaumgartner.couponbooster.coop.service;

import com.patbaumgartner.couponbooster.coop.properties.CoopPlaywrightProperties;
import com.patbaumgartner.couponbooster.coop.properties.CoopSelectorsProperties;
import com.patbaumgartner.couponbooster.coop.properties.CoopUserProperties;
import com.patbaumgartner.couponbooster.exception.CouponBoosterException;
import com.patbaumgartner.couponbooster.model.AuthenticationResult;
import com.patbaumgartner.couponbooster.util.proxy.ProxyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CoopAuthenticationService} focusing on credential validation and
 * configuration.
 */
@ExtendWith(MockitoExtension.class)
class CoopAuthenticationServiceTest {

	@Mock
	private DatadomeCaptchaResolver datadomeCaptchaResolver;

	@Mock
	private CoopBrowserFactory browserCreator;

	@Mock
	private DatadomeStealthInjector stealthInjector;

	private ProxyProperties proxyProperties;

	private CoopPlaywrightProperties browserConfiguration;

	private CoopSelectorsProperties elementSelectors;

	@BeforeEach
	void setUp() {
		proxyProperties = new ProxyProperties(false, "http://proxy.example.com");
		browserConfiguration = new CoopPlaywrightProperties("https://www.supercard.ch/login", "cookie-value", 100, 1000,
				false, List.of("--no-sandbox"), 20000, "/tmp/user-data", false, "");
		elementSelectors = new CoopSelectorsProperties("#login", "#username", "#password", "#submit", "#accept");
	}

	@Test
	void performAuthentication_withMissingEmail_shouldFailWithDetailedMessage() {
		// Given
		CoopUserProperties emptyEmailCredentials = new CoopUserProperties("", "validPassword");

		CoopAuthenticationService service = new CoopAuthenticationService(datadomeCaptchaResolver, proxyProperties,
				emptyEmailCredentials, browserConfiguration, elementSelectors, browserCreator, stealthInjector);

		// When
		AuthenticationResult result = service.performAuthentication();

		// Then
		assertThat(result.isSuccessful()).isFalse();
		assertThat(result.statusMessage()).contains("Email is missing");
	}

	@Test
	void performAuthentication_withMissingPassword_shouldFailWithDetailedMessage() {
		// Given
		CoopUserProperties emptyPasswordCredentials = new CoopUserProperties("test@example.com", "");

		CoopAuthenticationService service = new CoopAuthenticationService(datadomeCaptchaResolver, proxyProperties,
				emptyPasswordCredentials, browserConfiguration, elementSelectors, browserCreator, stealthInjector);

		// When
		AuthenticationResult result = service.performAuthentication();

		// Then
		assertThat(result.isSuccessful()).isFalse();
		assertThat(result.statusMessage()).contains("Password is missing");
	}

	@Test
	void performAuthentication_withMissingBothCredentials_shouldFailWithBothErrors() {
		// Given
		CoopUserProperties emptyCredentials = new CoopUserProperties("", "");

		CoopAuthenticationService service = new CoopAuthenticationService(datadomeCaptchaResolver, proxyProperties,
				emptyCredentials, browserConfiguration, elementSelectors, browserCreator, stealthInjector);

		// When
		AuthenticationResult result = service.performAuthentication();

		// Then
		assertThat(result.isSuccessful()).isFalse();
		assertThat(result.statusMessage()).contains("Email is missing");
		assertThat(result.statusMessage()).contains("Password is missing");
	}

	@Test
	void performAuthentication_withInvalidEmailFormat_shouldFailWithFormatError() {
		// Given
		CoopUserProperties invalidEmailCredentials = new CoopUserProperties("invalid-email", "validPassword");

		CoopAuthenticationService service = new CoopAuthenticationService(datadomeCaptchaResolver, proxyProperties,
				invalidEmailCredentials, browserConfiguration, elementSelectors, browserCreator, stealthInjector);

		// When
		AuthenticationResult result = service.performAuthentication();

		// Then
		assertThat(result.isSuccessful()).isFalse();
		assertThat(result.statusMessage()).contains("Email format is invalid");
	}

}
