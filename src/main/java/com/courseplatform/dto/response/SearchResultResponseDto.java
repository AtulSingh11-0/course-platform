package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Individual course search result with matched content")
public record SearchResultResponseDto (
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
		description = "List of matching subtopics within this course"
	)
	List< SearchMatchResponseDto > matches
) {}
