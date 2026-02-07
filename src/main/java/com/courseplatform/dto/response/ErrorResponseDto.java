package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard error response format")
public record ErrorResponseDto (
	@Schema(
		description = "Error type category (e.g., 'Bad Request', 'Unauthorized', 'Not Found')",
		example = "Not Found"
	)
	String error,

	@Schema(
		description = "Detailed error message explaining what went wrong",
		example = "Course with ID unknown-course not found"
	)
	String message,

	@Schema(
		description = "Timestamp when the error occurred",
		example = "2026-02-07T10:30:00Z",
		type = "string",
		format = "date-time"
	)
	Instant timestamp
) {}
