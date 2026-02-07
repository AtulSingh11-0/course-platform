package com.courseplatform.service;

import com.courseplatform.dto.response.EnrollmentResponseDto;

public interface EnrollmentService {
	EnrollmentResponseDto enrollUserInCourse(String email, String courseId);
}
