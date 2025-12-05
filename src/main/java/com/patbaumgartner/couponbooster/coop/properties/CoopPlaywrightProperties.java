package com.patbaumgartner.couponbooster.coop.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;

/**
 * Configuration properties for Playwright browser automation settings. Controls how the
 * browser behaves during the Coop authentication process.
 *
 * @param loginUrl the URL of the Coop login page to navigate to initially
 * @param datadomeCookieValue optional DataDome cookie value for bypassing bot detection
 * (no longer required with stealth measures)
 * @param typingDelayMs delay in milliseconds between individual keystrokes to simulate
 * human typing
 * @param slowMoMs general slowdown in milliseconds for all Playwright actions to appear
 * more human-like
 * @param headless whether to run the browser in headless mode (true) or with visible UI
 * (false)
 * @param chromeArgs list of Chrome browser arguments to customize browser behavior and
 * security settings
 * @param timeoutMs maximum time in milliseconds to wait for page elements and navigation
 * actions
 * @param userDataDir optional directory to store browser user data (cookies, local
 * storage). If set, a persistent browser context will be used.
 * @param guiMode when true, enables GUI-friendly mode optimized for desktop interaction
 * (OSX, Windows, Linux). Overrides headless to false and applies GUI-specific settings.
 * @param browserChannel optional browser channel to use (e.g., "chrome", "msedge",
 * "chrome-beta"). For OSX, using "chrome" uses system-installed Chrome.
 */
@ConfigurationProperties(prefix = "coop.playwright")
@Validated
public record CoopPlaywrightProperties(

		@NotBlank(message = "Login URL is required") @URL(message = "Login URL must be a valid URL") String loginUrl,

		// Changed: DataDome cookie is now optional - stealth measures should suffice
		String datadomeCookieValue,

		@Min(value = 0, message = "Typing delay cannot be negative") @Max(value = 1000,
				message = "Typing delay cannot exceed 1000ms") int typingDelayMs,

		@Min(value = 0, message = "Slow motion delay cannot be negative") @Max(value = 5000,
				message = "Slow motion delay cannot exceed 5000ms") int slowMoMs,

		boolean headless,

		@NotNull(message = "Chrome arguments list is required") List<String> chromeArgs,

		@Min(value = 1000, message = "Timeout must be at least 1000ms") @Max(value = 300000,
				message = "Timeout cannot exceed 300000ms (5 minutes)") int timeoutMs,

		String userDataDir,

		boolean guiMode,

		String browserChannel) {

	/**
	 * Custom constructor that creates a defensive copy of the chromeArgs list.
	 */
	public CoopPlaywrightProperties {
		chromeArgs = chromeArgs != null ? List.copyOf(chromeArgs) : List.of();
	}

	/**
	 * Returns effective headless mode considering GUI mode override.
	 * @return false if guiMode is enabled, otherwise the configured headless value
	 */
	public boolean effectiveHeadless() {
		return !guiMode && headless;
	}

	/**
	 * Checks if the current operating system is macOS.
	 * @return true if running on macOS, false otherwise
	 */
	public static boolean isMacOS() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
		return osName.contains("mac") || osName.contains("darwin");
	}

}
