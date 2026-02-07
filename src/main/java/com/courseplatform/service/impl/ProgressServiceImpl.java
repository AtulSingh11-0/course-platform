package com.courseplatform.service.impl;

import com.courseplatform.dto.response.ProgressReportResponseDto;
import com.courseplatform.dto.response.SubtopicCompletionResponseDto;
import com.courseplatform.model.SubtopicProgress;
import com.courseplatform.repository.EnrollmentRepository;
import com.courseplatform.repository.SubtopicProgressRepository;
import com.courseplatform.repository.SubtopicRepository;
import com.courseplatform.repository.UserRepository;
import com.courseplatform.service.ProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProgressServiceImpl implements ProgressService {

	private static final Logger log = LoggerFactory.getLogger(ProgressServiceImpl.class);

	private final SubtopicRepository subtopicRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final SubtopicProgressRepository subtopicProgressRepository;
	private final UserRepository userRepository;

	public ProgressServiceImpl(
		SubtopicRepository subtopicRepository,
		EnrollmentRepository enrollmentRepository,
		SubtopicProgressRepository subtopicProgressRepository,
		UserRepository userRepository
	) {
		this.subtopicRepository = subtopicRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.subtopicProgressRepository = subtopicProgressRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public SubtopicCompletionResponseDto markAsCompleted ( String userEmail, String subtopicId ) {
		// STEP-1: fetch user by email
		var user = userRepository.findByEmail(userEmail)
			.orElseThrow(() -> {
				log.error("User with email {} not found", userEmail);
				return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
			});

		// STEP-2: fetch subtopic by id and the parent course
		// STEP-2.1: fetch subtopic by id
		var subtopic = subtopicRepository.findById(subtopicId)
			.orElseThrow(() -> {
				log.error("Subtopic with id {} not found", subtopicId);
				return new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtopic not found");
			});
		// STEP-2.2: fetch parent course from subtopic
		var course = subtopic.getTopic().getCourse();

		// STEP-3: fetch enrollment by user and course
		var enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId())
			.orElseThrow(() -> {
				log.error("Enrollment not found for user id {} and course id {}", user.getId(), course.getId());
				return new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not enrolled in the course");
			});

		// STEP-4: if subtopic progress already exists, return response else create new progress
		return subtopicProgressRepository.findByEnrollmentIdAndSubtopicId(enrollment.getId(), subtopicId)
			.map( progress -> new SubtopicCompletionResponseDto(
				subtopicId,
				true,
				progress.getCompletedAt()
			))
			.orElseGet( () -> {
				// create new subtopic progress
				var progress = SubtopicProgress.builder()
					.enrollment(enrollment)
					.subtopic(subtopic)
					.build();
				progress.markCompleted();

				var savedProgress = subtopicProgressRepository.save(progress); // save it to DB
				log.info("Subtopic progress created for enrollment id {} and subtopic id {}", enrollment.getId(), subtopicId);

				// return response dto
				return new SubtopicCompletionResponseDto(
					subtopicId,
					true,
					savedProgress.getCompletedAt()
				);
			}
		);
	}

	@Override
	@Transactional(readOnly = true)
	public ProgressReportResponseDto getProgressReport ( String userEmail, Long enrollmentId ) {
		// STEP-1: fetch enrollment by id
		var enrollment = enrollmentRepository.findById(enrollmentId)
			.orElseThrow(() -> {
				log.error("Enrollment with id {} not found", enrollmentId);
				return new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found");
			});

		// STEP-2: verify that the enrollment belongs to the user
		if (!enrollment.getUser().getEmail().equals(userEmail)) {
			log.error("User with email {} is not authorized to access enrollment id {}", userEmail, enrollmentId);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
		}

		// STEP-3: get course from the enrollment
		var course = enrollment.getCourse();

		// STEP-4: calculate progress report totals
		// STEP-4.1: total subtopics in the course
		var totalSubtopics = course.getTopics().stream()
			.mapToInt(topic -> topic.getSubtopics().size())
			.sum();

		// STEP-4.2: total completed subtopics for the enrollment
		var progressList = subtopicProgressRepository.findByEnrollmentId(enrollmentId);
		int completedSubtopicsCount = progressList.size();

		// STEP-4.3: calculate percentage completed
		double percentageCompleted = totalSubtopics == 0 ? 0.0 :
			BigDecimal.valueOf((double) completedSubtopicsCount / totalSubtopics * 100)
				.setScale(2, RoundingMode.HALF_UP)
				.doubleValue();

		// STEP-4.4: map completed subtopic items
		var completeSubtopicItems = progressList.stream()
			.map(progress -> new ProgressReportResponseDto.CompletedSubtopicResponseDto(
				progress.getSubtopic().getId(),
				progress.getSubtopic().getTitle(),
				progress.getCompletedAt()
			))
			.toList();

		// STEP-5: return progress report response dto
		return new ProgressReportResponseDto(
			enrollment.getId(),
			course.getId(),
			course.getTitle(),
			totalSubtopics,
			completedSubtopicsCount,
			percentageCompleted,
			completeSubtopicItems
		);
	}
}
