package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Course summary information for listing")
public record CourseSummaryResponseDto (
	@Schema(
		description = "Unique course identifier (slug)",
		example = "physics-101"
	)
	String id,

	@Schema(
		description = "Course title",
		example = "Introduction to Physics"
	)
	String title,

	@Schema(
		description = "Brief course description",
		example = "Fundamental concepts of motion, forces, and energy."
	)
	String description,

	@Schema(
		description = "Total number of topics in the course",
		example = "3"
	)
	int topicCount,

	@Schema(
		description = "Total number of subtopics across all topics",
		example = "9"
	)
	int subtopicCount
) {}

