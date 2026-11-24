package com.busbooking.traffic;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings under {@code app.traffic.*} (see application.properties).
 *
 * @param provider       where traffic comes from: AUTO (Google if its key is set, else TomTom if its key is set,
 *                       else {@code withoutKey}), GOOGLE, TOMTOM, SIMULATED (made-up rush-hour numbers) or OFF
 * @param withoutKey     what AUTO falls back to with no key: OFF or SIMULATED
 * @param googleApiKey   server-side Google Maps key with the Routes API enabled. Never sent to browsers
 * @param tomtomApiKey   server-side TomTom key. Never sent to browsers
 * @param endpoint       Google Routes API computeRoutes URL (changed only in tests)
 * @param tomtomBaseUrl  TomTom API address (changed only in tests)
 * @param cacheTtl       how long one answer per route is reused before the provider is asked again
 * @param timeout        how long to wait for the provider
 * @param dailyLimit     most provider requests per day; after that the last known answer is served.
 *                       A negative number means no limit
 * @param speedIntervals Google only: also ask for per-section speeds. Off by default because Google may bill
 *                       it at a higher rate. TomTom always includes them
 * @param tripWindow     arrival estimates use live traffic only for trips leaving within this time
 */
@ConfigurationProperties("app.traffic")
public record TrafficProperties(Mode provider, Mode withoutKey, String googleApiKey, String tomtomApiKey,
                                String endpoint, String tomtomBaseUrl, Duration cacheTtl, Duration timeout,
                                Integer dailyLimit, boolean speedIntervals, Duration tripWindow) {

    public enum Mode { AUTO, GOOGLE, TOMTOM, SIMULATED, OFF }

    public static final String GOOGLE_ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";
    public static final String TOMTOM_BASE_URL = "https://api.tomtom.com";

    public TrafficProperties {
        if (provider == null) provider = Mode.AUTO;
        if (withoutKey != Mode.SIMULATED) withoutKey = Mode.OFF;
        if (googleApiKey != null && googleApiKey.isBlank()) googleApiKey = null;
        if (tomtomApiKey != null && tomtomApiKey.isBlank()) tomtomApiKey = null;
        if (endpoint == null || endpoint.isBlank()) endpoint = GOOGLE_ENDPOINT;
        if (tomtomBaseUrl == null || tomtomBaseUrl.isBlank()) tomtomBaseUrl = TOMTOM_BASE_URL;
        if (cacheTtl == null) cacheTtl = Duration.ofMinutes(5);
        if (timeout == null) timeout = Duration.ofSeconds(5);
        if (dailyLimit == null) dailyLimit = 160;
        if (tripWindow == null) tripWindow = Duration.ofHours(3);
    }

    /** AUTO resolved to the provider actually used. */
    public Mode effectiveMode() {
        if (provider != Mode.AUTO) return provider;
        if (googleApiKey != null) return Mode.GOOGLE;
        if (tomtomApiKey != null) return Mode.TOMTOM;
        return withoutKey;
    }
}
