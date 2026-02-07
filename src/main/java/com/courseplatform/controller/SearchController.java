package com.courseplatform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.courseplatform.dto.response.SearchResponseDto;
import com.courseplatform.search.service.SearchService;
import com.courseplatform.service.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Multi-strategy search operations (PostgreSQL, Elasticsearch, Semantic)")
public class SearchController {

	private final CourseService courseService;
	private final SearchService searchService;

	public SearchController(
		CourseService courseService,
		SearchService searchService
	) {
		this.courseService = courseService;
		this.searchService = searchService;
	}

	@GetMapping
	@Operation(
		summary = "Search courses (PostgreSQL)",
		description = "Searches courses using PostgreSQL full-text search. Searches across course titles, descriptions, and content. No authentication required."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Search completed successfully (may return empty results)",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = SearchResponseDto.class)
			)
		)
	})
	@Parameter(
		name = "q",
		description = "Search query string",
		required = false,
		example = "velocity",
		in = ParameterIn.QUERY,
		schema = @Schema(type = "string")
	)
	public ResponseEntity< SearchResponseDto > search(
		@RequestParam(
			name = "q",
			required = false
		) String query
	) {
		return ResponseEntity.ok(courseService.search(query));
	}

	@GetMapping("/semantic")
	@Operation(
		summary = "Search courses (Semantic)",
		description = "Searches courses using vector embeddings and semantic similarity. Understands meaning and context beyond keyword matching. No authentication required."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Semantic search completed successfully (may return empty results)",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = SearchResponseDto.class)
			)
		)
	})
	@Parameter(
		name = "q",
		description = "Semantic search query (natural language)",
		required = false,
		example = "rate of change",
		in = ParameterIn.QUERY,
		schema = @Schema(type = "string")
	)
	public ResponseEntity< SearchResponseDto > searchSemantic(
		@RequestParam(
			name = "q",
			required = false
		) String query
	) {
		return ResponseEntity.ok(courseService.searchSemantic(query));
	}

	@GetMapping("/es")
	@Operation(
		summary = "Search courses (Elasticsearch)",
		description = "Searches courses using Elasticsearch with advanced text analysis and ranking. Provides fast, relevance-ranked results. No authentication required."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Elasticsearch search completed successfully (may return empty results)",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = SearchResponseDto.class)
			)
		)
	})
	@Parameter(
		name = "q",
		description = "Elasticsearch query string",
		required = false,
		example = "Newton",
		in = ParameterIn.QUERY,
		schema = @Schema(type = "string")
	)
	public ResponseEntity< SearchResponseDto > searchElastic(
		@RequestParam(
			name = "q",
			required = false
		) String query
	) {
		return ResponseEntity.ok(searchService.searchCourses(query));
	}

}
