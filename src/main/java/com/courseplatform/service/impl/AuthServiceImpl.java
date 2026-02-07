package com.courseplatform.service.impl;

import com.courseplatform.dto.request.LoginRequestDto;
import com.courseplatform.dto.request.RegisterRequestDto;
import com.courseplatform.dto.response.AuthResponseDto;
import com.courseplatform.dto.response.RegisterResponseDto;
import com.courseplatform.model.User;
import com.courseplatform.repository.UserRepository;
import com.courseplatform.security.JwtUtil;
import com.courseplatform.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger log = LoggerFactory.getLogger( AuthServiceImpl.class );

	@Value("${jwt.expiration-ms}")
	private long expirationMs;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final AuthenticationManager authenticationManager;

	public AuthServiceImpl (
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		JwtUtil jwtUtil,
		AuthenticationManager authenticationManager
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
		this.authenticationManager = authenticationManager;
	}

	@Override
	public RegisterResponseDto register ( RegisterRequestDto request ) {
		// STEP-1: check if user with email already exists
		if (userRepository.findByEmail(request.email()).isPresent()) {
			log.error("User with email {} already exists", request.email());
			throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
		}

		// STEP-2: create user entity and save to database
		var user = User.builder()
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.build();
		var savedUser = userRepository.save(user);

		// STEP-3: prepare response
		return new RegisterResponseDto(
			savedUser.getId(),
			savedUser.getEmail(),
			"User registered successfully"
		);
	}

	@Override
	public AuthResponseDto login ( LoginRequestDto request ) {
		// STEP-1: authenticate user
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				request.email(),
				request.password()
			)
		);

		// STEP-2: check if user exists
		var user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> {
				log.error("User with email {} not found", request.email());
				return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
			});

		// STEP-3: generate JWT token
		var token = jwtUtil.generateToken(
			new org.springframework.security.core.userdetails.User(
				user.getEmail(),
				user.getPassword(),
				Collections.emptyList()
			)
		);

		// STEP-4: prepare response
		return new AuthResponseDto(
			token,
			user.getEmail(),
			expirationMs
		);
	}
}
