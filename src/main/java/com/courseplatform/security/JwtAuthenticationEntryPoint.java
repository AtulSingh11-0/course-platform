package com.courseplatform.security;

import com.courseplatform.dto.response.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public JwtAuthenticationEntryPoint ( ObjectMapper objectMapper ) {
		this.objectMapper = objectMapper;
	}

	@Override
	@NullMarked
	public void commence (
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		ErrorResponseDto errorResponse = new ErrorResponseDto(
			"Unauthorized",
			"JWT token is missing or invalid",
			Instant.now()
		);

		objectMapper.writeValue(response.getOutputStream(), errorResponse);
	}
}
