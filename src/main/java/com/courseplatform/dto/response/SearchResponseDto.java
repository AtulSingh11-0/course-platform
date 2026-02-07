package com.courseplatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Search results response")
public record SearchResponseDto (
	@Schema(
		description = "Original search query",
		example = "velocity"
	)
	String query,

	@Schema(
		description = "List of courses matching the search query"
	)
	List< SearchResultResponseDto > results
) {}
