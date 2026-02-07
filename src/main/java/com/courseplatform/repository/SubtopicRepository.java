package com.courseplatform.repository;

import com.courseplatform.model.Subtopic;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtopicRepository extends JpaRepository< Subtopic, String > {
	@Override
	@NullMarked
	@EntityGraph(attributePaths = {"topic", "topic.course"})
	List<Subtopic> findAllById(Iterable<String> ids);
}
