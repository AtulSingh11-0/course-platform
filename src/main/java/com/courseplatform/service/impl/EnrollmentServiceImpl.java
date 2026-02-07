package com.courseplatform.service.impl;

import com.courseplatform.dto.response.EnrollmentResponseDto;
import com.courseplatform.model.Enrollment;
import com.courseplatform.repository.CourseRepository;
import com.courseplatform.repository.EnrollmentRepository;
import com.courseplatform.repository.UserRepository;
import com.courseplatform.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

	private static final Logger log = LoggerFactory.getLogger( EnrollmentServiceImpl.class );

	private final UserRepository userRepository;
	private final CourseRepository courseRepository;
	private final EnrollmentRepository enrollmentRepository;

	public EnrollmentServiceImpl (
		UserRepository userRepository,
		CourseRepository courseRepository,
		EnrollmentRepository enrollmentRepository
	) {
		this.userRepository = userRepository;
		this.courseRepository = courseRepository;
		this.enrollmentRepository = enrollmentRepository;
	}

	@Override
	@Transactional
	public EnrollmentResponseDto enrollUserInCourse ( String email, String courseId ) {
		// STEP-1: validate user and course existence by fetching user and course

		// STEP-1.1: fetch user by email
		var user = userRepository.findByEmail( email )
			.orElseThrow( () -> {
				log.error( "User with email {} not found", email );
				return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found" );
			} );

		// STEP-1.2: fetch course by courseId
		var course = courseRepository.findById( courseId )
			.orElseThrow( () -> {
				log.error( "Course with id {} not found", courseId );
				return new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found" );
			} );

		// STEP-2: check if user is already enrolled in the course
		boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId( user.getId(), courseId );
		if ( isEnrolled ) {
			log.error( "User with email {} is already enrolled in course {}", email, courseId );
			throw new ResponseStatusException( HttpStatus.CONFLICT, "User is already enrolled in the course" );
		}

		// STEP-3: create and save enrollment
		var enrollment = Enrollment.builder()
			.user( user )
			.course( course )
			.build();

		var savedEnrollment = enrollmentRepository.save( enrollment );

		// STEP-4: prepare and return response DTO
		return new EnrollmentResponseDto(
			savedEnrollment.getId(),
			course.getId(),
			course.getTitle(),
			savedEnrollment.getCreatedAt()
		);
	}
}
