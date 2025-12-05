package com.patbaumgartner.couponbooster.coop.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CoopPlaywrightProperties} focusing on GUI mode and effective
 * headless behavior.
 */
class CoopPlaywrightPropertiesTest {

	@Test
	void effectiveHeadless_withGuiModeEnabled_shouldReturnFalse() {
		// Given
		CoopPlaywrightProperties properties = new CoopPlaywrightProperties("https://login.url", "cookie", 100, 1000,
				true, // headless = true
				List.of("--arg1"), 20000, "/user-data", true, // guiMode = true
				"chrome");

		// When
		boolean effectiveHeadless = properties.effectiveHeadless();

		// Then
		assertThat(effectiveHeadless).isFalse();
	}

	@Test
	void effectiveHeadless_withGuiModeDisabled_shouldReturnConfiguredHeadless() {
		// Given - headless true, GUI mode false
		CoopPlaywrightProperties propertiesHeadless = new CoopPlaywrightProperties("https://login.url", "cookie", 100,
				1000, true, // headless = true
				List.of("--arg1"), 20000, "/user-data", false, // guiMode = false
				"");

		// Given - headless false, GUI mode false
		CoopPlaywrightProperties propertiesNotHeadless = new CoopPlaywrightProperties("https://login.url", "cookie",
				100, 1000, false, // headless = false
				List.of("--arg1"), 20000, "/user-data", false, // guiMode = false
				"");

		// When & Then
		assertThat(propertiesHeadless.effectiveHeadless()).isTrue();
		assertThat(propertiesNotHeadless.effectiveHeadless()).isFalse();
	}

	@Test
	void chromeArgs_shouldBeImmutable() {
		// Given
		List<String> originalArgs = List.of("--arg1", "--arg2");
		CoopPlaywrightProperties properties = new CoopPlaywrightProperties("https://login.url", "cookie", 100, 1000,
				false, originalArgs, 20000, "/user-data", false, "");

		// When
		List<String> args = properties.chromeArgs();

		// Then
		assertThat(args).containsExactly("--arg1", "--arg2");
	}

	@Test
	void chromeArgs_withNull_shouldCreateEmptyList() {
		// Given
		CoopPlaywrightProperties properties = new CoopPlaywrightProperties("https://login.url", "cookie", 100, 1000,
				false, null, 20000, "/user-data", false, "");

		// When
		List<String> args = properties.chromeArgs();

		// Then
		assertThat(args).isEmpty();
	}

	@Test
	void browserChannel_shouldBeAccessible() {
		// Given
		CoopPlaywrightProperties properties = new CoopPlaywrightProperties("https://login.url", "cookie", 100, 1000,
				false, List.of(), 20000, "/user-data", true, "chrome");

		// When & Then
		assertThat(properties.browserChannel()).isEqualTo("chrome");
	}

	@Test
	void guiMode_shouldBeAccessible() {
		// Given
		CoopPlaywrightProperties propertiesGuiEnabled = new CoopPlaywrightProperties("https://login.url", "cookie", 100,
				1000, false, List.of(), 20000, "/user-data", true, "");

		CoopPlaywrightProperties propertiesGuiDisabled = new CoopPlaywrightProperties("https://login.url", "cookie",
				100, 1000, false, List.of(), 20000, "/user-data", false, "");

		// When & Then
		assertThat(propertiesGuiEnabled.guiMode()).isTrue();
		assertThat(propertiesGuiDisabled.guiMode()).isFalse();
	}

}
