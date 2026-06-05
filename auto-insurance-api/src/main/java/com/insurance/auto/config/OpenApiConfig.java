package com.insurance.auto.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title = "Auto Insurance Premium Calculation API",
        version = "1.0.0",
        description = "API REST para cálculo de prêmio de seguro automotivo com análise de risco multivariável. " +
                      "Integra com ViaCEP para localização, aplica multiplicadores regionais e classifica perfil de risco.",
        contact = @Contact(
            name = "Insurance Platform Team",
            email = "api@insurance.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Servidor de Desenvolvimento")
    }
)
@Configuration
public class OpenApiConfig {
}
