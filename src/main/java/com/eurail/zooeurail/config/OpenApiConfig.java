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

    private static final String API_DESCRIPTION = """
            HTTP JSON API for managing Animals and Rooms in a Zoo.

            Features:
            - CRUD for single Animal/Room entities (no list endpoints).
            - Place/Move/Remove animals in rooms.
            - Assign/Unassign favorite rooms to animals.
            - Query animals in a room with sorting and pagination.
            - Aggregate favorite rooms with counts (titles only).
            """;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Zoo API")
                        .description(API_DESCRIPTION)
                        .version("v1")
                        .license(new License().name("Apache-2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new Contact().name("Zoo API Maintainers"))
                );
    }
}
