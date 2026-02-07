package com.courseplatform.controller;

import com.courseplatform.dto.response.CourseDetailResponseDto;
import com.courseplatform.dto.response.CourseSummaryResponseDto;
import com.courseplatform.dto.response.EnrollmentResponseDto;
import com.courseplatform.dto.response.ErrorResponseDto;
import com.courseplatform.service.CourseService;
import com.courseplatform.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Course browsing and enrollment operations")
public class CourseController {

	private static final Logger log = LoggerFactory.getLogger(CourseController.class);

	private final CourseService courseService;
	private final EnrollmentService enrollmentService;

	public CourseController(
		CourseService courseService,
		EnrollmentService enrollmentService
	) {
		this.courseService = courseService;
		this.enrollmentService = enrollmentService;
	}

	@GetMapping
	@Operation(
		summary = "List all courses",
		description = "Retrieves a summary list of all available courses including title, description, and content counts. No authentication required."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully retrieved course list",
			content = @Content(
				mediaType = "application/json",
				array = @ArraySchema(schema = @Schema(implementation = CourseSummaryResponseDto.class))
			)
		)
	})
	public ResponseEntity< List< CourseSummaryResponseDto > > getAllCourseSummaries() {
		return ResponseEntity.ok(courseService.getAllCourseSummaries());
	}

	@GetMapping("/{courseId}" )
	@Operation(
		summary = "Get course details",
		description = "Retrieves detailed information about a specific course including all topics and subtopics. No authentication required."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully retrieved course details",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = CourseDetailResponseDto.class)
			)
		),
		@ApiResponse(
			responseCode = "404",
			description = "Course not found",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Not Found",
                          "message": "Course not found",
                          "timestamp": "2025-12-21T10:30:00Z"
                        }
                    """
				)
			)
		)
	})
	@Parameter(
		name = "courseId",
		description = "Course identifier (unique slug)",
		required = true,
		example = "physics-101",
		in = ParameterIn.PATH
	)
	public ResponseEntity< CourseDetailResponseDto > getCourseDetailById(
		@PathVariable String courseId
	) {
		return ResponseEntity.ok(courseService.getCourseDetailById(courseId));
	}

	@PostMapping("/{courseId}/enroll" )
	@Operation(
		summary = "Enroll in a course",
		description = "Enrolls the authenticated user in the specified course. Creates a new enrollment record to track learning progress.",
		security = @SecurityRequirement(name = "Bearer Authentication")
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully enrolled in course",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = EnrollmentResponseDto.class)
			)
		),
		@ApiResponse(
			responseCode = "409",
			description = "User already enrolled in course",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Conflict",
                          "message": "User is already enrolled in the course",
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
			description = "Course not found",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Not Found",
                          "message": "Course not found",
                          "timestamp": "2026-02-07T10:30:00Z"
                        }
                    """
				)
			)
		)
	})
	@Parameter(
		name = "courseId",
		description = "Course identifier to enroll in",
		required = true,
		example = "java-basics",
		in = ParameterIn.PATH
	)
	public ResponseEntity< EnrollmentResponseDto > enrollUserInCourse(
		@PathVariable String courseId,
		Principal principal
	) {
		String email = getEmailFromPrincipal(principal);
		return ResponseEntity.ok(enrollmentService.enrollUserInCourse(email, courseId));
	}

	private String getEmailFromPrincipal(Principal principal) {
		return Optional.ofNullable(principal)
			.map(Principal::getName)
			.orElseThrow(() -> {
				log.error("Principal is null, cannot retrieve email");
				return new ResponseStatusException(
					HttpStatus.UNAUTHORIZED,
					"User is not authenticated"
				);
			});
	}
}
