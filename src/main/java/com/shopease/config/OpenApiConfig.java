package com.shopease.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI shopEaseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopEase API")
                        .version("1.0.0")
                        .description("""
                                Multi-vendor e-commerce backend.

                                **How to test in Swagger UI**
                                1. `POST /api/auth/register` (role CUSTOMER or VENDOR) or `POST /api/auth/login`.
                                2. Copy the `accessToken` from the response.
                                3. Click the green **Authorize** button and paste the token (no "Bearer " prefix needed).
                                4. Call any protected endpoint.

                                Default admin: `admin@shopease.com` / `Admin@12345` (change via env vars).
                                Guest cart: send any unique string in the `X-Session-Id` header.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .name(SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
