package com.courseplatform.repository;

import com.courseplatform.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

	interface SearchResultInterface {
		String getCourseId();
		String getCourseTitle();
		String getMatchType();
		String getTopicTitle();
		String getSubtopicId();
		String getSubtopicTitle();
		String getContentSnippet();
		Double getScore();
	}

	@Query(value = """
        SELECT * FROM (
            -- 1. Match Course Title (Weighted 10x)
            SELECT c.id as courseId, c.title as courseTitle, 'course' as matchType,
                   CAST(NULL as text) as topicTitle, CAST(NULL as text) as subtopicId, CAST(NULL as text) as subtopicTitle,
                   c.description as contentSnippet,
                   (word_similarity(:query, c.title) * 10) as score
            FROM courses c
            WHERE word_similarity(:query, c.title) > 0.3
               OR c.title ILIKE '%' || :query || '%'

            UNION ALL

            -- 2. Match Topic Title (Weighted 5x)
            SELECT c.id, c.title, 'topic',
                   t.title, NULL, NULL, NULL,
                   (word_similarity(:query, t.title) * 5) as score
            FROM topics t
            JOIN courses c ON t.course_id = c.id
            WHERE word_similarity(:query, t.title) > 0.3
               OR t.title ILIKE '%' || :query || '%'

            UNION ALL

            -- 3. Match Subtopic Title (Weighted 5x)
            SELECT c.id, c.title, 'subtopic',
                   t.title, s.id, s.title, NULL,
                   (word_similarity(:query, s.title) * 5) as score
            FROM subtopics s
            JOIN topics t ON s.topic_id = t.id
            JOIN courses c ON t.course_id = c.id
            WHERE word_similarity(:query, s.title) > 0.3
               OR s.title ILIKE '%' || :query || '%'

            UNION ALL

            -- 4. Match Content (Weighted 1x)
            -- Content is too large for fuzzy matching every word efficiently,
            -- so we stick to ILIKE for speed, or strict word matching.
            SELECT c.id, c.title, 'content',
                   t.title, s.id, s.title, s.content,
                   1.0 as score
            FROM subtopics s
            JOIN topics t ON s.topic_id = t.id
            JOIN courses c ON t.course_id = c.id
            WHERE s.content ILIKE '%' || :query || '%'
        ) AS unified_results
        ORDER BY score DESC
        LIMIT 20
        """, nativeQuery = true)
	List<SearchResultInterface> searchGlobal(@Param("query") String query);
}
