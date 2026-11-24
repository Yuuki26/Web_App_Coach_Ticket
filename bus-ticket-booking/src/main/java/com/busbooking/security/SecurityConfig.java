package com.busbooking.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Who may call what.
 * <ul>
 *   <li>Stateless: no server session. Each request carries {@code Authorization: Bearer <access token>}.</li>
 *   <li>CSRF protection is off because the token is sent in a header, never in a cookie, so another
 *       site cannot make the browser send it automatically.</li>
 *   <li>Public: the page, trip search, seat maps, quotes and guest bookings, auth endpoints, API docs.</li>
 *   <li>Everything else needs a valid token; role checks are on the controllers ({@code @PreAuthorize}).</li>
 *   <li>401/403 errors go through GlobalExceptionHandler so they have the same JSON shape as other errors.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Pages and files served by this app. */
    private static final String[] STATIC_FILES = {"/", "/index.html", "/app.js", "/styles.css", "/favicon.ico", "/error"};
    private static final String[] API_DOCS = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(STATIC_FILES).permitAll()
                        .requestMatchers(API_DOCS).permitAll()
                        .requestMatchers("/h2-console/**").permitAll() // only enabled in the "h2" profile
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/cities", "/api/v1/routes/**", "/api/v1/trips/**").permitAll()
                        // Guest checkout: booking and managing a booking by code + email stays open.
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings", "/api/v1/bookings/quote",
                                "/api/v1/bookings/lookup", "/api/v1/bookings/cancel").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/manage/**").hasAnyRole("MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((req, res, ex) -> resolver.resolveException(req, res, null, ex))
                        .accessDeniedHandler((req, res, ex) -> resolver.resolveException(req, res, null, ex)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> resolver.resolveException(req, res, null, ex))
                        .accessDeniedHandler((req, res, ex) -> resolver.resolveException(req, res, null, ex)))
                .headers(h -> h
                        .frameOptions(f -> f.sameOrigin()) // H2 console uses frames
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline'; "       // page uses inline onclick handlers
                                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                        + "font-src 'self' https://fonts.gstatic.com; "
                                        + "img-src 'self' data: https://images.unsplash.com; "
                                        + "frame-ancestors 'self'")));
        return http.build();
    }

    /** Reads the "roles" claim of the token as ROLE_CUSTOMER, ROLE_ADMIN, ... */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /** BCrypt with cost 12 (2^12 rounds); the salt is generated per password and stored in the hash. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
