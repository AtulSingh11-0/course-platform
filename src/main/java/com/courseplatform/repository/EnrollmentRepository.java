package com.courseplatform.repository;

import com.courseplatform.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository< Enrollment, Long > {
	boolean existsByUserIdAndCourseId(Long userId, String courseId);

	@Query ("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.course.id = :courseId AND e.user.id = :userId")
	Optional< Enrollment > findByUserIdAndCourseId(
		@Param("userId") Long userId,
		@Param("courseId") String courseId
	);
}
