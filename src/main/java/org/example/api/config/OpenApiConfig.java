package org.example.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        String serverUrl = activeProfile.equals("prod")
                ? "https://api.smartecommerce.example.com"
                : "http://localhost:8080";

        return new OpenAPI()
                .info(new Info()
                        .title("Smart E-Commerce API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for the Smart E-Commerce System.

                                **Features**
                                - User, product, and category management
                                - Paginated, filterable, and sortable product catalog
                                - Bean Validation with custom cross-field constraints
                                - Environment-aware profiles (dev / test / prod)

                                All responses follow the envelope:
                                ```json
                                { "status": "success|error", "message": "...", "data": { ... } }
                                ```
                                """)
                        .contact(new Contact()
                                .name("Smart E-Commerce Team")
                                .email("dev@smartecommerce.example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(serverUrl).description(activeProfile + " server")
                ));
    }
}
