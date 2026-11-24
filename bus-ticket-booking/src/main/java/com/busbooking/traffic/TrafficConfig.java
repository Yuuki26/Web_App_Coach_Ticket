package com.busbooking.traffic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(TrafficProperties.class)
public class TrafficConfig {

    private static final Logger log = LoggerFactory.getLogger(TrafficConfig.class);

    /** Days for the daily limit and rush hours for the simulation follow Vietnam time. */
    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    TrafficProvider trafficProvider(TrafficProperties props) {
        TrafficProperties.Mode mode = props.effectiveMode();
        TrafficProvider provider = switch (mode) {
            case GOOGLE -> {
                if (props.googleApiKey() == null) {
                    throw new IllegalStateException(
                            "app.traffic.provider=google needs a key: set the GOOGLE_MAPS_API_KEY environment variable");
                }
                yield new GoogleRoutesProvider(props);
            }
            case TOMTOM -> {
                if (props.tomtomApiKey() == null) {
                    throw new IllegalStateException(
                            "app.traffic.provider=tomtom needs a key: set the TOMTOM_API_KEY environment variable");
                }
                yield new TomTomTrafficProvider(props);
            }
            case SIMULATED -> new SimulatedTrafficProvider(Clock.system(VIETNAM));
            default -> new DisabledTrafficProvider();
        };
        String described = switch (mode) {
            case GOOGLE -> "Google Routes API (answers reused for " + props.cacheTtl().toMinutes() + " min, limit "
                    + (props.dailyLimit() < 0 ? "none" : props.dailyLimit() + " requests/day") + ")";
            case TOMTOM -> "TomTom Routing API (answers reused for " + props.cacheTtl().toMinutes() + " min, limit "
                    + (props.dailyLimit() < 0 ? "none" : props.dailyLimit() + " requests/day") + ")";
            case SIMULATED -> "SIMULATED numbers, not real traffic";
            default -> "off (set TOMTOM_API_KEY or GOOGLE_MAPS_API_KEY to turn it on)";
        };
        log.info("Live traffic: {}", described);
        return provider;
    }
}
