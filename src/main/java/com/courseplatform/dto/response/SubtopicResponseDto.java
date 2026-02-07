package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Individual learning unit within a topic")
public record SubtopicResponseDto (
	@Schema(
		description = "Unique subtopic identifier",
		example = "speed"
	)
	String id,

	@Schema(
		description = "Subtopic title",
		example = "Speed"
	)
	String title,

	@Schema(
		description = "Learning content for this subtopic (markdown format)",
		example = "Speed is the distance travelled per unit time.\\n\\nIt is a scalar quantity..."
	)
	String content
) {}
