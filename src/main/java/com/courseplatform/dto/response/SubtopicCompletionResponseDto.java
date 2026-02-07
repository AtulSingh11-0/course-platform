package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

@Schema(description = "Subtopic completion status")
public record SubtopicCompletionResponseDto (
	@Schema(
		description = "ID of the completed subtopic",
		example = "velocity"
	)
	String subtopicId,

	@Schema(
		description = "Completion status flag",
		example = "true"
	)
	boolean completed,

	@Schema(
		description = "Timestamp when the subtopic was marked as completed",
		example = "2025-12-21T10:30:00Z",
		type = "string",
		format = "date-time"
	)
	ZonedDateTime completedAt
) {}
