package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Specific content match within a course")
public record SearchMatchResponseDto (
	@Schema(
		description = "Type of match (e.g., 'subtopic', 'title', 'description')",
		example = "subtopic"
	)
	String type,

	@Schema(
		description = "Parent topic title",
		example = "Kinematics"
	)
	String topicTitle,

	@Schema(
		description = "Matched subtopic identifier",
		example = "velocity"
	)
	String subtopicId,

	@Schema(
		description = "Matched subtopic title",
		example = "Velocity"
	)
	String subtopicTitle,

	@Schema(
		description = "Content snippet showing the match context",
		example = "Velocity is the rate of change of displacement..."
	)
	String snippet
//	Double score
) {}
