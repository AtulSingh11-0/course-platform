package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import java.util.List;

@Schema(description = "Detailed progress report for a course enrollment")
public record ProgressReportResponseDto (
	@Schema(
		description = "Enrollment identifier",
		example = "123"
	)
	Long enrollmentId,

	@Schema(
		description = "Course identifier",
		example = "physics-101"
	)
	String courseId,

	@Schema(
		description = "Course title",
		example = "Introduction to Physics"
	)
	String courseTitle,

	@Schema(
		description = "Total number of subtopics in the course",
		example = "9"
	)
	int totalSubtopics,

	@Schema(
		description = "Number of subtopics completed by the user",
		example = "5"
	)
	int completedSubtopics,

	@Schema(
		description = "Progress completion percentage (0-100)",
		example = "55.56"
	)
	double completionPercentage,

	@Schema(
		description = "List of completed subtopics with timestamps"
	)
	List< CompletedSubtopicResponseDto > completedItems
) {
	@Schema(description = "Completed subtopic details")
	public record CompletedSubtopicResponseDto (
		@Schema(
			description = "Subtopic identifier",
			example = "speed"
		)
		String subtopicId,

		@Schema(
			description = "Subtopic title",
			example = "Speed"
		)
		String subtopicTitle,

		@Schema(
			description = "Completion timestamp",
			example = "2025-12-20T14:20:00Z",
			type = "string",
			format = "date-time"
		)
		ZonedDateTime completedAt
	) {}
}
