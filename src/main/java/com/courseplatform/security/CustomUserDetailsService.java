package com.courseplatform.security;

import com.courseplatform.model.User;
import com.courseplatform.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

	private final UserRepository userRepository;

	public CustomUserDetailsService ( UserRepository userRepository ) {
		this.userRepository = userRepository;
	}

	@Override
	@NullMarked
	public UserDetails loadUserByUsername ( String email ) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email)
			.orElseThrow( () -> {
				log.error("User not found with email: {}", email);
				return new UsernameNotFoundException("User not found with email: " + email);
			});

		return new org.springframework.security.core.userdetails.User(
			user.getEmail(),
			user.getPassword(),
			new ArrayList<>() // empty list of authorities i.e. no roles for now
		);
	}
}
