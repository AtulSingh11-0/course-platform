package com.courseplatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration request data")
public record RegisterRequestDto (
	@Schema(
		description = "Email address for the new account",
		example = "student@example.com",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	String email,

	@Schema(
		description = "Password for the new account (should be at least 8 characters)",
		example = "securePassword123",
		requiredMode = Schema.RequiredMode.REQUIRED,
		format = "password",
		minLength = 8
	)
	@NotBlank(message = "Password is required")
	@Size(min = 6, message = "Password must be at least 6 characters long")
	String password
) {}
