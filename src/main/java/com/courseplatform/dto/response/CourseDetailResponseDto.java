package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Detailed course information including all topics and subtopics")
public record CourseDetailResponseDto (
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
		description = "Detailed course description",
		example = "Fundamental concepts of motion, forces, and energy."
	)
	String description,

	@Schema(
		description = "List of topics within the course, each containing subtopics"
	)
	List<TopicResponseDto> topics
) {}
