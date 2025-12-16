package com.eurail.zooeurail.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Zoo API")
                        .description("HTTP JSON API for managing Animals and Rooms in a Zoo.\n\n"
                                + "Features:\n"
                                + "- CRUD for single Animal/Room entities (no list endpoints).\n"
                                + "- Place/Move/Remove animals in rooms.\n"
                                + "- Assign/Unassign favorite rooms to animals.\n"
                                + "- Query animals in a room with sorting and pagination.\n"
                                + "- Aggregate favorite rooms with counts (titles only).")
                        .version("v1")
                        .license(new License().name("Apache-2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new Contact().name("Zoo API Maintainers"))
                );
    }
}
