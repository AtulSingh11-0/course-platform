package com.courseplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtUtil jwtUtil;
	private final CustomUserDetailsService userDetailsService;

	public JwtAuthenticationFilter (
		JwtUtil jwtUtil,
		CustomUserDetailsService userDetailsService
	) {
		this.jwtUtil = jwtUtil;
		this.userDetailsService = userDetailsService;
	}

	@Override
	@NullMarked
	protected void doFilterInternal (
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		final String authHeader = request.getHeader("Authorization");
		final String jwt;
		final String userEmail;

		// check if the request has Authorization header and starts with "Bearer " or not
		if ( authHeader == null || !authHeader.startsWith("Bearer ") ) {
			filterChain.doFilter(request, response); // continue filter chain
			return;
		}

		// STEP-1: extract jwt token from the header
		jwt = authHeader.substring(7); // remove "Bearer " prefix to get the token

		// try to authenticate the user using the token
		try {
			// STEP-2: extract username (email) from the token
			userEmail = jwtUtil.extractUsername(jwt);

			// STEP-3: check if user is authenticated or not yet in the security context
			if (
				userEmail != null &&
				SecurityContextHolder.getContext().getAuthentication() == null
			) {
				// STEP-3.1: load user details from database
				var userDetails = userDetailsService.loadUserByUsername(userEmail);

				// STEP-3.2: validate the token
				if ( jwtUtil.isTokenValid(jwt, userDetails) ) {
					// STEP-3.3: create authentication token
					var authToken = new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
					);

					// STEP-3.4: set details of the authentication token
					authToken.setDetails(
						new WebAuthenticationDetailsSource().buildDetails(request)
					);

					// STEP-3.5: set authentication in the security context
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch ( Exception e ) { // catch any exception during authentication
			log.error("Failed to authenticate user: {}", e.getMessage(), e);
		}
		// STEP-4: continue the filter chain
		filterChain.doFilter(request, response);
	}
}
