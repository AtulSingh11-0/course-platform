package com.courseplatform.config;

import com.courseplatform.security.CustomUserDetailsService;
import com.courseplatform.security.JwtAuthenticationEntryPoint;
import com.courseplatform.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final CustomUserDetailsService userDetailsService;
	private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;

	public SecurityConfig (
		JwtAuthenticationFilter jwtAuthFilter,
		CustomUserDetailsService userDetailsService,
		JwtAuthenticationEntryPoint jwtAuthEntryPoint
	) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.userDetailsService = userDetailsService;
		this.jwtAuthEntryPoint = jwtAuthEntryPoint;
	}

	private final String[] whitelistEndpoints = {
		"/error",			// allow error path
		"/api/auth/**",     // allow all auth related endpoints
		"/api/courses/**",  // allow all course related endpoints
		"/api/search/**",   // allow all search related endpoints
		"/v3/api-docs/**",  // allow OpenAPI docs
		"/swagger-ui/**",   // allow Swagger UI
		"/swagger-ui/index.html"  // allow Swagger UI HTML
	};

	@Bean
	public SecurityFilterChain securityFilterChain( HttpSecurity http ) {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(whitelistEndpoints).permitAll()
				.anyRequest().authenticated()
			)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authenticationProvider(authenticationProvider())
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(
			userDetailsService
		);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager( AuthenticationConfiguration config ) {
		return config.getAuthenticationManager();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
