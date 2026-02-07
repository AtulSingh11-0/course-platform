package com.courseplatform.search.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@Document(indexName = "courses")
public class CourseDocument {

	@Id
	private String id;

	@Field(type = FieldType.Text, analyzer = "english")
	private String title;

	@Field(type = FieldType.Text, analyzer = "english")
	private String description;

	@Field(type = FieldType.Nested)
	private List<TopicSearchItem> topics;

	@Data
	@Builder
	public static class TopicSearchItem {

		@Field(type = FieldType.Keyword)
		private String id;

		@Field(type = FieldType.Text, analyzer = "english")
		private String title;

		@Field(type = FieldType.Nested, analyzer = "english")
		private List<SubtopicSearchItem> subtopics;
	}

	@Data
	@Builder
	public static class SubtopicSearchItem {

		@Field(type = FieldType.Keyword)
		private String id;

		@Field(type = FieldType.Text, analyzer = "english")
		private String title;

		@Field(type = FieldType.Text, analyzer = "english")
		private String content;
	}
}
