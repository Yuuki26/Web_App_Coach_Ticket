package com.busbooking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation. Spec: /v3/api-docs · UI: /swagger-ui.html.
 * Use "Authorize" in the UI and paste an access token from POST /api/v1/auth/login.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI hCoachOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("H Coach API")
                        .version("v1")
                        .description("Coach ticket booking: trips, seats, bookings and accounts. "
                                + "Errors are RFC 7807 JSON with an extra \"code\" field."))
                .components(new Components().addSecuritySchemes("bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
