package com.courseplatform.repository;

import com.courseplatform.model.SubtopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtopicProgressRepository extends JpaRepository< SubtopicProgress, Long > {
	Optional< SubtopicProgress > findByEnrollmentIdAndSubtopicId(Long enrollmentId, String subtopicId);
	List< SubtopicProgress > findByEnrollmentId(Long enrollmentId);
}
