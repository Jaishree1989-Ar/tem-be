package com.tem.be.api.utils;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("COS Telecom Expense Management")
                        .description("This API provides endpoints to manage users, roles, and TELECOM reports.")
                        .version("1.0.0"));
    }
}