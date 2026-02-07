package com.courseplatform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Course Platform API")
				.version("1.0")
				.description("""
        Backend API for the Course Learning Platform.

        ### Features:
        - **Public:** Browse courses, search (PostgreSQL + Elasticsearch + Semantic).
        - **Authenticated:** Enroll, track progress.

        **Note:** Authentication is JWT-based. Use the 'Authorize' button.
        """)
				.contact(new Contact()
					.name("Atul Singh")
					.email("cocatul11@gmail.com")))
			.components(new Components()
				.addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
	}

	private SecurityScheme createAPIKeyScheme() {
		return new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.bearerFormat("JWT")
			.scheme("bearer");
	}
}