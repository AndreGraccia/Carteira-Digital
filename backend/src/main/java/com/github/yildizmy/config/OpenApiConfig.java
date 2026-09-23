package com.github.yildizmy.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
// Based on yildizmy/e-wallet by Murat Yıldız (MIT); credits kept in LICENSE.
@OpenAPIDefinition(info = @Info(title = "Wall.ia REST API", version = "1.0",
        description = "Carteira que projeta compromissos, explica cada número e simula decisões sem mover dinheiro",
        contact = @Contact(name = "Wall.ia")),
        security = {@SecurityRequirement(name = "bearerToken")}
)
@SecurityScheme(name = "bearerToken", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
