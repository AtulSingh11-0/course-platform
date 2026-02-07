package com.courseplatform.exception;

import com.courseplatform.dto.response.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// error type constants
	private static final String ERROR_DEFAULT = "Error";
	private static final String ERROR_CONFLICT = "Conflict";
	private static final String ERROR_NOT_FOUND = "Not Found";
	private static final String ERROR_FORBIDDEN = "Forbidden";
	private static final String ERROR_BAD_REQUEST = "Bad Request";
	private static final String ERROR_UNAUTHORIZED = "Unauthorized";
	private static final String ERROR_METHOD_NOT_ALLOWED = "Method Not Allowed";
	private static final String ERROR_INTERNAL_SERVER_ERROR = "Internal Server Error";

	// error message constants
	private static final String MSG_ACCESS_DENIED = "Access denied";
	private static final String MSG_INVALID_CREDENTIALS = "Invalid email or password";
	private static final String MSG_UNEXPECTED_ERROR = "An unexpected error occurred";
	private static final String MSG_RESOURCE_NOT_FOUND = "The requested resource was not found";
	private static final String MSG_METHOD_NOT_ALLOWED = "The HTTP method is not supported for this endpoint";

	// used in services when throwing exceptions with specific status codes
	@ExceptionHandler ( ResponseStatusException.class )
	public ResponseEntity< ErrorResponseDto > handleResponseStatusException ( ResponseStatusException ex ) {
		return buildResponse(ex.getStatusCode().value(), getErrorType(ex.getStatusCode().value()), ex.getReason());
	}

	// to handle login failures eg: bad credentials
	@ExceptionHandler ( BadCredentialsException.class )
	public ResponseEntity< ErrorResponseDto > handleBadCredentials ( BadCredentialsException ex ) {
		log.error("Bad Credentials: ", ex);
		return buildResponse(HttpStatus.UNAUTHORIZED.value(), ERROR_UNAUTHORIZED, MSG_INVALID_CREDENTIALS);
	}

	// handle generic security exceptions
	@ExceptionHandler ( AccessDeniedException.class )
	public ResponseEntity< ErrorResponseDto > handleSpringAccessDenied ( AccessDeniedException ex ) {
		log.error("Spring Security Access Denied: ", ex);
		return buildResponse(HttpStatus.FORBIDDEN.value(), ERROR_FORBIDDEN, MSG_ACCESS_DENIED);
	}

	// handle user not found
	@ExceptionHandler ( UsernameNotFoundException.class )
	public ResponseEntity< ErrorResponseDto > handleUserNotFound ( UsernameNotFoundException ex ) {
		return buildResponse(HttpStatus.UNAUTHORIZED.value(), ERROR_UNAUTHORIZED, ex.getMessage());
	}

	// handle 404 - no handler found exception
	@ExceptionHandler ( NoHandlerFoundException.class )
	public ResponseEntity< ErrorResponseDto > handleNoHandlerFound ( NoHandlerFoundException ex ) {
		log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
		return buildResponse(HttpStatus.NOT_FOUND.value(), ERROR_NOT_FOUND, MSG_RESOURCE_NOT_FOUND);
	}

	// handle 404 - no resource found exception (spring 6+)
	@ExceptionHandler ( NoResourceFoundException.class )
	public ResponseEntity< ErrorResponseDto > handleNoResourceFound ( NoResourceFoundException ex ) {
		log.warn("No resource found: {}", ex.getMessage());
		return buildResponse(HttpStatus.NOT_FOUND.value(), ERROR_NOT_FOUND, MSG_RESOURCE_NOT_FOUND);
	}

	// handle 405 - method not supported
	@ExceptionHandler ( HttpRequestMethodNotSupportedException.class )
	public ResponseEntity< ErrorResponseDto > handleMethodNotSupported ( HttpRequestMethodNotSupportedException ex ) {
		log.warn("Method not supported: {}", ex.getMessage());
		return buildResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), ERROR_METHOD_NOT_ALLOWED, MSG_METHOD_NOT_ALLOWED);
	}

	// handle validation errors (400)
	@ExceptionHandler ( MethodArgumentNotValidException.class )
	public ResponseEntity< ErrorResponseDto > handleValidationErrors ( MethodArgumentNotValidException ex ) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.findFirst()
			.orElse("Validation failed");
		log.warn("Validation error: {}", message);
		return buildResponse(HttpStatus.BAD_REQUEST.value(), ERROR_BAD_REQUEST, message);
	}

	// handle type mismatch errors (400)
	@ExceptionHandler ( MethodArgumentTypeMismatchException.class )
	public ResponseEntity< ErrorResponseDto > handleTypeMismatch ( MethodArgumentTypeMismatchException ex ) {
		String message = String.format("Invalid value for parameter '%s'", ex.getName());
		log.warn("Type mismatch: {}", message);
		return buildResponse(HttpStatus.BAD_REQUEST.value(), ERROR_BAD_REQUEST, message);
	}

	// fallback for all other unhandled exceptions (internal server error)
	@ExceptionHandler ( Exception.class )
	public ResponseEntity< ErrorResponseDto > handleGenericException ( Exception ex ) {
		// log the full stack trace for debugging on the server console
		log.error("An unexpected error occurred: ", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_INTERNAL_SERVER_ERROR, MSG_UNEXPECTED_ERROR);
	}

	// helper method to build the JSON response
	private ResponseEntity< ErrorResponseDto > buildResponse ( int status, String errorType, String message ) {
		ErrorResponseDto response = new ErrorResponseDto(
			errorType,
			message,
			Instant.now()
		);
		return ResponseEntity.status(status).body(response);
	}

	// helper to map status codes to readable error types
	private String getErrorType ( int status ) {
		return switch ( status ) {
			case 400 -> ERROR_BAD_REQUEST;
			case 401 -> ERROR_UNAUTHORIZED;
			case 403 -> ERROR_FORBIDDEN;
			case 404 -> ERROR_NOT_FOUND;
			case 405 -> ERROR_METHOD_NOT_ALLOWED;
			case 409 -> ERROR_CONFLICT;
			case 500 -> ERROR_INTERNAL_SERVER_ERROR;
			default -> ERROR_DEFAULT;
		};
	}
}
