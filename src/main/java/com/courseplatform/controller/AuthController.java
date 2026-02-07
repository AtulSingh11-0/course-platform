package com.courseplatform.controller;

import com.courseplatform.dto.request.LoginRequestDto;
import com.courseplatform.dto.request.RegisterRequestDto;
import com.courseplatform.dto.response.AuthResponseDto;
import com.courseplatform.dto.response.ErrorResponseDto;
import com.courseplatform.dto.response.RegisterResponseDto;
import com.courseplatform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration operations")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@Operation(
		summary = "Register a new user",
		description = "Creates a new user account with email and password. Returns user details upon successful registration."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "201",
			description = "User successfully registered",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = RegisterResponseDto.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "Validation Error",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = {
					@ExampleObject(
						name = "Invalid Email",
						summary = "Error: Invalid Email Format",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "email: Invalid email format",
                              "timestamp": "2026-02-06T22:39:02.642624100Z"
                            }
                        """
					),
					@ExampleObject(
						name = "Missing Email",
						summary = "Error: Email is required",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "email: Email is required",
                              "timestamp": "2026-02-06T22:39:15.986998700Z"
                            }
                        """
					),
					@ExampleObject(
						name = "Short Password",
						summary = "Error: Password too short",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "password: Password must be at least 6 characters long",
                              "timestamp": "2026-02-06T22:33:55.084635300Z"
                            }
                        """
					),
					@ExampleObject(
						name = "Missing Password",
						summary = "Error: Password is required",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "password: Password is required",
                              "timestamp": "2026-02-06T22:39:36.553905300Z"
                            }
                        """
					)
				}
			)
		),
		@ApiResponse(
			responseCode = "409",
			description = "User with this email already exists",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Conflict",
                          "message": "User with email student@example.com already exists",
                          "timestamp": "2026-02-06T22:40:00.000000000Z"
                        }
                    """
				)
			)
		)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "User registration credentials",
		required = true,
		content = @Content(
			mediaType = "application/json",
			schema = @Schema(implementation = RegisterRequestDto.class)
		)
	)
	public ResponseEntity<RegisterResponseDto> register(
		@Valid @RequestBody RegisterRequestDto request
	) {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(authService.register(request));
	}

	@PostMapping("/login")
	@Operation(
		summary = "Authenticate user",
		description = "Authenticates a user with email and password. Returns a JWT token valid for 24 hours."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully authenticated",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = AuthResponseDto.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "Invalid request body (Missing Fields)",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = {
					@ExampleObject(
						name = "Invalid Email",
						summary = "Error: Invalid Email Format",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "email: Invalid email format",
                              "timestamp": "2026-02-06T22:39:02.642624100Z"
                            }
                        """
					),
					@ExampleObject(
						name = "Missing Email",
						summary = "Error: Email is required",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "email: Email is required",
                              "timestamp": "2026-02-06T22:39:15.986998700Z"
                            }
                        """
					),
					@ExampleObject(
						name = "Short Password",
						summary = "Error: Password too short",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "password: Password must be at least 6 characters long",
                              "timestamp": "2026-02-06T22:33:55.084635300Z"
                            }
                        """
					),
					@ExampleObject(
						name = "Missing Password",
						summary = "Error: Password is required",
						value = """
                            {
                              "error": "Bad Request",
                              "message": "password: Password is required",
                              "timestamp": "2026-02-06T22:39:36.553905300Z"
                            }
                        """
					)
				}
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "Invalid credentials",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ErrorResponseDto.class),
				examples = @ExampleObject(
					value = """
                        {
                          "error": "Unauthorized",
                          "message": "Invalid email or password",
                          "timestamp": "2026-02-06T22:41:00.000000000Z"
                        }
                    """
				)
			)
		)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "User login credentials",
		required = true,
		content = @Content(
			mediaType = "application/json",
			schema = @Schema(implementation = LoginRequestDto.class)
		)
	)
	public ResponseEntity<AuthResponseDto> login(
		@Valid @RequestBody LoginRequestDto request
	) {
		return ResponseEntity.ok(authService.login(request));
	}
}