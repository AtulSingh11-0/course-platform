package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration response")
public record RegisterResponseDto (
	@Schema(
		description = "Unique identifier for the newly created user",
		example = "1"
	)
	Long id,

	@Schema(
		description = "Email address of the registered user",
		example = "student@example.com"
	)
	String email,

	@Schema(
		description = "Success message",
		example = "User registered successfully"
	)
	String message
) {}
