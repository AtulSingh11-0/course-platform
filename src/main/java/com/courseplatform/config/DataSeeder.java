package com.courseplatform.config;

import com.courseplatform.model.Course;
import com.courseplatform.repository.CourseRepository;
import com.courseplatform.search.document.CourseDocument;
import com.courseplatform.search.repository.CourseSearchRepository;
import com.courseplatform.service.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

	private static final String DATA_FILE_PATH = "/data/courses.json";
	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final ObjectMapper objectMapper;
	private final CourseRepository courseRepository; // jpa
	private final EmbeddingService embeddingService;
	private final CourseSearchRepository courseSearchRepository; // elasticsearch

	public DataSeeder (
		ObjectMapper objectMapper,
		CourseRepository courseRepository,
		EmbeddingService embeddingService,
		CourseSearchRepository courseSearchRepository
	) {
		this.objectMapper = objectMapper;
		this.courseRepository = courseRepository;
		this.embeddingService = embeddingService;
		this.courseSearchRepository = courseSearchRepository;
	}

	@Override
	@Transactional
	public void run ( String @NonNull ... args ) {
		// STEP-1: SEED DB
		// check if db is empty or not
		if ( courseRepository.count() == 0 ) {
			log.info("Database empty, Seeding initial data into the database...");
			// since db is empty, we need to seed initial data
			try { // try to read JSON file and parse it
				var courses = loadCoursesFromJson(); // try to load courses from json file
				courseRepository.saveAll(courses); // save to db
				log.info("Successfully seeded {} courses", courses.size());
			} catch ( IOException e ) { // catch any exception during file read or parse
				log.error("Failed to seed initial data: {}", e.getMessage(), e);
				// handling the error gracefully without crashing the app, as the app can still function without initial data (just empty)
			}
		}

		log.info("Database is not empty, skipping data seeding.");

		// STEP-2: SYNC TO ELASTICSEARCH
		// this ensures that even if the app is restarted and elasticsearch index is lost, it will be re-indexed from the database
		try {
			log.info("Syncing courses to Elasticsearch...");
			syncCoursesToElasticsearch();
		} catch ( Exception ex ) {
			log.error("Failed to sync courses to Elasticsearch: {}", ex.getMessage(), ex);
			 // handling the error gracefully without crashing the app, as the app can still function without elasticsearch (just search will not work)
		}

		// STEP-3: GENERATE EMBEDDINGS
		// will be generating the embeddings after data is loaded
		embeddingService.initializeEmbeddings();
	}

	private List< Course > loadCoursesFromJson() throws IOException {
		// read JSON file from resources
		InputStream inputStream = TypeReference.class.getResourceAsStream( DATA_FILE_PATH );

		// check if inputStream is null or not basically file exists or not
		if ( inputStream == null ) {
			throw new IOException("Data file not found: " + DATA_FILE_PATH);
		}

		// parse the wrapper object in which courses are contained
		var rootNode = objectMapper.readTree(inputStream);
		var coursesArray = rootNode.get("courses");

		// parse courses array to list<Course>
		var courses = objectMapper.readValue(
			objectMapper.writeValueAsString(coursesArray),
			new TypeReference< List< Course > >() {}
		);

		/*
		* set up bidirectional relationships between courses, topics, and subtopics manually before saving
		* as jackson does not handle it automatically
		* */
		for ( Course course : courses ) {
			if (course.getTopics() != null) {
				course.getTopics().forEach(topic -> {
					topic.setCourse(course); // link topic back to course

					if (topic.getSubtopics() != null) {
						topic.getSubtopics().forEach(subtopic -> subtopic.setTopic(topic)); // link subtopic back to topic
					}
				});
			}
		}

		return courses;
	}

	private void syncCoursesToElasticsearch() {
		// c fetch all courses from db
		var courses = courseRepository.findAll();

		// STEP-2: convert courses to course documents for elasticsearch
		var documents = courses.stream()
			.map(course -> CourseDocument.builder()
					.id(course.getId())
					.title(course.getTitle())
					.description(course.getDescription())
					.topics(course.getTopics().stream()
						.map(topic -> CourseDocument.TopicSearchItem.builder()
								.id(topic.getId())
								.title(topic.getTitle())
								.subtopics(topic.getSubtopics().stream()
									.map(subtopic -> CourseDocument.SubtopicSearchItem.builder()
										.id(subtopic.getId())
										.title(subtopic.getTitle())
										.content(subtopic.getContent())
										.build()
									)
									.toList())
							.build()
						)
						.toList())
				.build()
			)
			.toList();

		// STEP-3: save to elasticsearch
		courseSearchRepository.saveAll(documents);
		log.info("-------- Successfully indexed {} courses to Elasticsearch --------", documents.size());
	}
}
