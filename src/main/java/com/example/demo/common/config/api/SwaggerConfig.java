package com.example.demo.common.config.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo API")
                        .version("1.0.0"));
    }

    @Bean
    public GroupedOpenApi generalApi() {
        return GroupedOpenApi.builder()
                .group("general")
                .pathsToMatch("/api/**")
                .build();
    }
}
