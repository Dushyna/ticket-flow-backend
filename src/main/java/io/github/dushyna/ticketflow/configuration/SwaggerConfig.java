package io.github.dushyna.ticketflow.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    public static final String BEARER_AUTH = "bearerAuth";
    public static final String COOKIE_AUTH = "cookieAuth";

    @Bean
    public OpenAPI ticketFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketFlow SaaS API")
                        .version("1.0.0")
                        .description("Multi-tenant Booking System API. Supports OAuth2, JWT in Cookies, and Bearer Tokens.")
                        .contact(new Contact().name("Dushyna").url("https://github.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes(COOKIE_AUTH,
                                new SecurityScheme()
                                        .name("ACCESS_TOKEN")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)));
    }
}
