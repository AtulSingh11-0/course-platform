package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Course enrollment confirmation")
public record EnrollmentResponseDto (
	@Schema(
		description = "Unique enrollment identifier",
		example = "123"
	)
	Long enrollmentId,

	@Schema(
		description = "ID of the enrolled course",
		example = "physics-101"
	)
	String courseId,

	@Schema(
		description = "Title of the enrolled course",
		example = "Introduction to Physics"
	)
	String courseTitle,

	@Schema(
		description = "Timestamp when the enrollment was created",
		example = "2025-12-21T09:00:00Z",
		type = "string",
		format = "date-time"
	)
	Instant enrolledAt
) {}
