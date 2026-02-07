package com.courseplatform.search.service.impl;

import com.courseplatform.dto.response.SearchMatchResponseDto;
import com.courseplatform.dto.response.SearchResponseDto;
import com.courseplatform.dto.response.SearchResultResponseDto;
import com.courseplatform.search.document.CourseDocument;
import com.courseplatform.search.service.SearchService;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.courseplatform.common.util.SnippetUtils.generateSnippet;

@Service
public class SearchServiceImpl implements SearchService {

	private final ElasticsearchOperations elasticsearchOperations;

	public SearchServiceImpl(ElasticsearchOperations elasticsearchOperations) {
		this.elasticsearchOperations = elasticsearchOperations;
	}

	private final List<String> courseSearchFields = List.of(
		"title^3",
		"description^2",
		"content"
	);

	private final List<String> topicSearchFields = List.of(
		"topics.title^2"
	);

	private final List<String> subtopicSearchFields = List.of(
		"topics.subtopics.title^2",
		"topics.subtopics.content"
	);

	@Override
	public SearchResponseDto searchCourses ( String query ) {
		// STEP-1: will need to build the query (Fuzzy + Boosting)
		// STEP-1.1: root query: search across course title and description
		var rootQuery = NativeQuery.builder()
			.withQuery(q -> q
				.multiMatch(m -> m
					.query(query)
					.fields(courseSearchFields)
					.fuzziness("AUTO")
				)
			)
			.build().getQuery();

		// STEP-1.2: topic query: search inside topics (nested)
		var topicQuery = NativeQuery.builder()
			.withQuery(q -> q
				.nested(n -> n
					.path("topics")
					.query(nq -> nq
						.multiMatch(m -> m
							.query(query)
							.fields(topicSearchFields)
							.fuzziness("AUTO")
						)
					)
				)
			)
			.build().getQuery();

		// STEP-1.3: subtopic query: search inside subtopics (deeply nested)
		var subtopicQuery = NativeQuery.builder()
			.withQuery(q -> q
				.nested(n -> n
					.path("topics")
					.query(nq -> nq
						.nested(sn -> sn
							.path("topics.subtopics")
							.query(nq2 -> nq2
								.multiMatch(m -> m
									.query(query)
									.fields(subtopicSearchFields)
									.fuzziness("AUTO")
								)
							)
						)
					)
				)
			)
			.build().getQuery();

		// STEP-1.4: combine the queries using a bool query with boosting
		var finalQuery = NativeQuery.builder()
			.withQuery(q -> q
				.bool(b -> b
					.should(rootQuery) // course title and description
					.should(topicQuery) // topic titles
					.should(subtopicQuery) // subtopic titles and content
				)
			)
			.build();

		// STEP-2: execute the query and get the results
		var searchHits = elasticsearchOperations.search(finalQuery, CourseDocument.class);

		// STEP-3: map the results to SearchResponseDto
		var results = searchHits.getSearchHits().stream()
			.map(hit -> mapToResult(hit, query))
			.filter(res -> !res.matches().isEmpty()) // only include results with matches
			.toList();

		return new SearchResponseDto(query, results);
	}

	private SearchResultResponseDto mapToResult( SearchHit<CourseDocument> hit, String query ) {
		String lowerCaseQuery = query.toLowerCase(); // for case-insensitive matching
		var matches = new ArrayList<SearchMatchResponseDto>(); // to store matched fields

		// STEP-1: get the document from the search hit
		var doc = hit.getContent();

		// STEP-2: check course title
//		if ( doc.getTitle().toLowerCase().contains(lowerCaseQuery) ) {
		if ( isFuzzyMatch(doc.getTitle(), query) ) {
			matches.add(new SearchMatchResponseDto(
				"course",
				null,
				null,
				null,
				"Matched in course title"
			));
		}

		// STEP-3: iterate through topics and subtopics to find matches
		// check topics
		if ( doc.getTopics() != null ) {
			doc.getTopics().forEach(topic -> {
				// check topic title
//				if ( topic.getTitle().toLowerCase().contains(lowerCaseQuery) ) {
				if ( isFuzzyMatch(topic.getTitle(), query) ) {
					matches.add(new SearchMatchResponseDto(
						"topic",
						topic.getTitle(),
						null,
						null,
						"Matched in topic title"
					));
				}
				// check subtopics
				if ( topic.getSubtopics() != null ) {
					// check subtopic title and content
					topic.getSubtopics().forEach(subtopic -> {
						// check subtopic title
//						if ( subtopic.getTitle().toLowerCase().contains(lowerCaseQuery) ) {
						if ( isFuzzyMatch(subtopic.getTitle(), query) ) {
							matches.add(new SearchMatchResponseDto(
								"subtopic",
								topic.getTitle(),
								subtopic.getId(),
								subtopic.getTitle(),
								"Matched in subtopic title"
							));
						} else if (
							subtopic.getContent() != null &&
							subtopic.getContent().toLowerCase().contains(lowerCaseQuery)
						) {
							// check subtopic content
							String snippet = generateSnippet(subtopic.getContent(), lowerCaseQuery);
							matches.add(new SearchMatchResponseDto(
								"subtopic",
								topic.getTitle(),
								subtopic.getId(),
								subtopic.getTitle(),
								snippet
							));
						}
					});
				}
			});
		}

		return new SearchResultResponseDto(
			doc.getId(),
			doc.getTitle(),
			matches
		);
	}

	private boolean isFuzzyMatch( String text, String query ) {
		if ( text == null || query == null ) {
			return false;
		}

		// Simple fuzzy match implementation: check if the text contains the query
		// with a Levenshtein distance of 1 or 2
		String lowerText = text.toLowerCase();
		String lowerQuery = query.toLowerCase();

		// fast check: if the text contains the query directly (case-insensitive)
		if ( lowerText.contains(lowerQuery) ) {
			return true;
		}

		// Check for simple typos (Levenshtein distance of 1)
		String[] words = lowerText.split("[^a-zA-Z0-9]+");
		for ( String word : words ) {
			if (Math.abs(word.length() - lowerQuery.length()) <= 2) {
				int distance = calculateLevenshteinDistance(word, lowerQuery);
				// Allow 1 typo for short words, 2 for longer queries
				int allowedEdits = ( lowerQuery.length() < 5 ) ? 1 : 2;
				if ( distance <= allowedEdits ) {
					return true;
				}
			}
		}

		return false;
	}

	private int calculateLevenshteinDistance( String s1, String s2 ) {
		int[][] dp = new int[s1.length() + 1][s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
		for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

		for (int i = 1; i <= s1.length(); i++) {
			for (int j = 1; j <= s2.length(); j++) {
				int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
				dp[i][j] = Math.min(
					Math.min(
						dp[i - 1][j] + 1,       // Deletion
						dp[i][j - 1] + 1        // Insertion
					),
					dp[i - 1][j - 1] + cost     // Substitution
				);
			}
		}

		return dp[s1.length()][s2.length()];
	}
}
