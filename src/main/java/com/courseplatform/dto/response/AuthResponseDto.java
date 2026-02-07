package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT token")
public record AuthResponseDto (
	@Schema(
		description = "JWT bearer token for authenticating subsequent requests",
		example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
	)
	String token,

	@Schema(
		description = "Email address of the authenticated user",
		example = "student@example.com"
	)
	String email,

	@Schema(
		description = "Token expiration time in seconds from now",
		example = "86400"
	)
	long expiresIn
) {}
