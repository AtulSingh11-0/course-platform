package com.courseplatform.service.impl;

import com.courseplatform.dto.response.*;
import com.courseplatform.model.Subtopic;
import com.courseplatform.repository.CourseRepository;
import com.courseplatform.repository.CourseRepository.SearchResultInterface;
import com.courseplatform.repository.SubtopicRepository;
import com.courseplatform.service.CourseService;
import com.courseplatform.service.EmbeddingService;
import com.courseplatform.service.EmbeddingService.ScoredSubtopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.courseplatform.common.util.SnippetUtils.generateSnippet;

@Service
public class CourseServiceImpl implements CourseService {

	private static final Logger log = LoggerFactory.getLogger( CourseServiceImpl.class );

	private final CourseRepository courseRepository;
	private final SubtopicRepository subtopicRepository;
	private final EmbeddingService embeddingService;

	public CourseServiceImpl (
		CourseRepository courseRepository,
		SubtopicRepository subtopicRepository,
		EmbeddingService embeddingService
	) {
		this.courseRepository = courseRepository;
		this.subtopicRepository = subtopicRepository;
		this.embeddingService = embeddingService;
	}

	@Override
	@Transactional(readOnly = true)
	public List< CourseSummaryResponseDto > getAllCourseSummaries () {
		return courseRepository.findAll().stream()
			.map(course -> new CourseSummaryResponseDto(
				course.getId(),
				course.getTitle(),
				course.getDescription(),
				course.getTopics().size(),
				course.getTopics().stream()
					.mapToInt(topic -> topic.getSubtopics().size())
					.sum()
				)
			)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public CourseDetailResponseDto getCourseDetailById ( String courseId ) {
		var course = courseRepository.findById(courseId)
			.orElseThrow( () -> {
				log.error("Course with id {} not found", courseId);
				return new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
			} );

		var topicDtos = course.getTopics().stream()
			.map(topic -> new TopicResponseDto(
					topic.getId(),
					topic.getTitle(),
					topic.getSubtopics().stream()
						.map(subtopic -> new SubtopicResponseDto(
								subtopic.getId(),
								subtopic.getTitle(),
								subtopic.getContent()
							)
						)
						.toList()
				)
			)
			.toList();

		return new CourseDetailResponseDto(
			course.getId(),
			course.getTitle(),
			course.getDescription(),
			topicDtos
		);
	}

	@Override
	@Transactional(readOnly = true)
	public SearchResponseDto search ( String query ) {
		// STEP-1: validate the query
		if ( isInvalidSearchQuery(query) ) {
			return new SearchResponseDto( query, List.of() );
		}

		// STEP-2: fetch raw results from the DB
		var rawResults = courseRepository.searchGlobal(query);

		// STEP-3: group results by courseId to match the response structure
		var groupedResults = rawResults.stream()
			.collect(
				Collectors.groupingBy(
					SearchResultInterface::getCourseId,
					LinkedHashMap::new,
					Collectors.toList()
				)
			);

		// STEP-4: transform grouped results into SearchResultResponseDto
		var searchResultDtos = groupedResults.entrySet().stream()
			.map( entry -> {
				var courseId = entry.getKey(); // courseId is the map key
				var courseTitle = entry.getValue().getFirst().getCourseTitle(); // all entries share the same courseTitle

				// transform each match into SearchMatchResponseDto
				var matchDtos = entry.getValue().stream()
					.map( match -> new SearchMatchResponseDto(
							match.getMatchType(),
							match.getTopicTitle(),
							match.getSubtopicId(),
							match.getSubtopicTitle(),
							generateSnippet(match.getContentSnippet(), query)
//							match.getScore()
						)
					)
					.toList();

				// construct and return SearchResultResponseDto
				return new SearchResultResponseDto(
					courseId,
					courseTitle,
					matchDtos
				);
			})
			.toList();

		// STEP-5: construct and return the final SearchResponseDto
		return new SearchResponseDto( query, searchResultDtos );
	}

	@Override
	@Transactional(readOnly = true)
	public SearchResponseDto searchSemantic ( String query ) {
		// STEP-1: validate the query
		if ( isInvalidSearchQuery(query) ) {
			return new SearchResponseDto( query, List.of() );
		}

		// STEP-2: get top 20 similar subtopics from embedding service
		var scoredSubtopics = embeddingService.searchBySimilarity(query, 20);
		if ( scoredSubtopics.isEmpty() ) {
			return new SearchResponseDto( query, List.of() );
		}

		// STEP-3: fetch full subtopic details
		var subtopicIds = scoredSubtopics.stream()
			.map(ScoredSubtopic::subtopicId)
			.toList();

		var subtopicsMap = subtopicRepository.findAllById(subtopicIds).stream()
			.collect(Collectors.toMap(Subtopic::getId, Function.identity()));

		// STEP-4: create matches sorted by relevance score (highest first)
		var matchesWithCourse = scoredSubtopics.stream()
			.map(scored -> {
				var subtopic = subtopicsMap.get(scored.subtopicId());
				if ( subtopic == null ) return null;

				var course = subtopic.getTopic().getCourse();
				return new SearchMatchWithCourse(
					course.getId(),
					course.getTitle(),
					new SearchMatchResponseDto(
						"semantic",
						subtopic.getTopic().getTitle(),
						subtopic.getId(),
						subtopic.getTitle(),
						generateSnippet(subtopic.getContent(), query)
//						scored.score()
					)
				);
			})
			.filter(Objects::nonNull)
			.toList();

		// STEP-5: group by course while maintaining overall score order and transform to SearchResultResponseDto
		var searchResultResponseDtos = matchesWithCourse.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.groupingBy(
				searchMatchWithCourse -> new CourseKey(
					searchMatchWithCourse.courseId(),
					searchMatchWithCourse.courseTitle()
				), // grouping by simple immutable key to preserve insertion order
				LinkedHashMap::new, // preserve insertion order to maintain relevance sorting
				Collectors.mapping(SearchMatchWithCourse::match, Collectors.toList())
			))
			.entrySet().stream()
			.map(entry ->  new SearchResultResponseDto(
					entry.getKey().courseId,
					entry.getKey().courseTitle,
					entry.getValue()
				)
			)
			.toList();

		return new SearchResponseDto( query, searchResultResponseDtos );
	}

	private boolean isInvalidSearchQuery ( String query ) {
		if ( query == null || query.isBlank() ) {
			log.warn("Search query is null or blank");
			return true;
		}
		return false;
	}

	// helper record to carry course info with matches
	private record SearchMatchWithCourse (
		String courseId,
		String courseTitle,
		SearchMatchResponseDto match
	) {}

	// helper record to group by course while preserving insertion order for relevance sorting
	private record CourseKey (
		String courseId,
		String courseTitle
	) {}
}
