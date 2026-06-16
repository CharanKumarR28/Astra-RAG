package com.groq.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI chatbotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java Chatbot API")
                        .description("Groq AI powered chatbot backend built with Spring Boot")
                        .version("1.0.0"));
    }
}
