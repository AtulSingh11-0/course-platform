package com.courseplatform.controller;

import com.courseplatform.dto.response.ErrorResponseDto;
import com.courseplatform.dto.response.ProgressReportResponseDto;
import com.courseplatform.dto.response.SubtopicCompletionResponseDto;
import com.courseplatform.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "Progress", description = "Learning progress tracking operations")
public class ProgressController {

	private static final Logger log = LoggerFactory.getLogger(ProgressController.class);

	private final ProgressService progressService;

	public ProgressController(ProgressService progressService) {
		this.progressService = progressService;
	}

	@PostMapping("/subtopics/{subtopicId}/complete")
	@Operation(
		summary = "Mark subtopic as completed",
		description = "Records the completion of a subtopic for the authenticated user. Timestamps the completion for progress tracking.",
		security = @SecurityRequirement(name = "Bearer Authentication")
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Subtopic marked as completed",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = SubtopicCompletionResponseDto.class)
			)
		),
		@ApiResponse(
			responseCode = "403",
			description = "User not enrolled in course",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Forbidden",
                          "message": "User is not enrolled in the course",
                          "timestamp": "2025-12-21T10:30:00Z"
                        }
                    """
				)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "Not authenticated - JWT token missing or invalid",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Unauthorized",
                          "message": "User is not authenticated",
                          "timestamp": "2025-12-21T10:30:00Z"
                        }
                    """
				)
			)
		),
		@ApiResponse(
			responseCode = "404",
			description = "Subtopic not found",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Not Found",
                          "message": "Subtopic not found",
                          "timestamp": "2025-12-21T10:30:00Z"
                        }
                    """
				)
			)
		)
	})
	@Parameter(
		name = "subtopicId",
		description = "Subtopic identifier to mark as completed",
		required = true,
		example = "velocity",
		in = ParameterIn.PATH
	)
	public ResponseEntity< SubtopicCompletionResponseDto > markAsCompleted(
		@PathVariable String subtopicId,
		Principal principal
	) {
		String email = getEmailFromPrincipal(principal);
		return ResponseEntity.ok(progressService.markAsCompleted(email, subtopicId));
	}

	@GetMapping("/enrollments/{enrollmentId}/progress")
	@Operation(
		summary = "Get progress report",
		description = "Retrieves a detailed progress report for a specific enrollment, including completion percentage and list of completed subtopics. User can only access their own enrollments.",
		security = @SecurityRequirement(name = "Bearer Authentication")
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully retrieved progress report",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ProgressReportResponseDto.class)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "Not authenticated - JWT token missing or invalid",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Unauthorized",
                          "message": "User is not authenticated",
                          "timestamp": "2025-12-21T10:30:00Z"
                        }
                    """
				)
			)
		),
		@ApiResponse(
			responseCode = "403",
			description = "Access denied - enrollment belongs to another user",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Forbidden",
                          "message": "Access denied",
                          "timestamp": "2025-12-21T10:30:00Z"
                        }
                    """
				)
			)
		),
		@ApiResponse(
			responseCode = "404",
			description = "Enrollment not found",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Not Found",
                          "message": "Enrollment not found",
                          "timestamp": "2025-12-21T10:30:00Z"
                        }
                    """
				)
			)
		)
	})
	@Parameter(
		name = "enrollmentId",
		description = "Enrollment ID to retrieve progress for (numeric identifier)",
		required = true,
		example = "123",
		in = ParameterIn.PATH,
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity< ProgressReportResponseDto > getProgressReport(
		@PathVariable Long enrollmentId,
		Principal principal
	) {
		String email = getEmailFromPrincipal(principal);
		return ResponseEntity.ok(progressService.getProgressReport(email, enrollmentId));
	}

	private String getEmailFromPrincipal(Principal principal) {
		return Optional.ofNullable(principal)
			.map(Principal::getName)
			.orElseThrow( () -> {
				log.error("Principal is null, cannot retrieve email");
				return new ResponseStatusException(
					HttpStatus.UNAUTHORIZED,
					"User is not authenticated"
				);
			});
	}
}
