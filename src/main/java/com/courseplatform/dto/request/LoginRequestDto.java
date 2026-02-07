package com.courseplatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User login credentials")
public record LoginRequestDto (
	@Schema(
		description = "User's email address",
		example = "student@example.com",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	@NotBlank (message = "Email is required")
	@Email (message = "Invalid email format")
	String email,

	@Schema(
		description = "User's password",
		example = "securePassword123",
		requiredMode = Schema.RequiredMode.REQUIRED,
		format = "password"
	)
	@NotBlank(message = "Password is required")
	@Size (min = 6, message = "Password must be at least 6 characters long")
	String password
) {}
