package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Course topic containing multiple subtopics")
public record TopicResponseDto (
	@Schema(
		description = "Unique topic identifier",
		example = "kinematics"
	)
	String id,

	@Schema(
		description = "Topic title",
		example = "Kinematics"
	)
	String title,

	@Schema(
		description = "List of subtopics within this topic"
	)
	List<SubtopicResponseDto> subtopics
) {}
