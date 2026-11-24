package com.busbooking.config;

import com.busbooking.security.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/** Turns on the automatic created/updated columns (see model.Auditable). */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditor")
public class AuditConfig {

    /** The logged-in user's email; "system" for startup data, guests and self sign-up. */
    @Bean
    AuditorAware<String> auditor() {
        return () -> Optional.of(CurrentUser.email().orElse(CurrentUser.SYSTEM));
    }
}
