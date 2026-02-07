package com.courseplatform.service.impl;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import com.courseplatform.model.Subtopic;
import com.courseplatform.repository.SubtopicRepository;
import com.courseplatform.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
	private static final int EMBEDDING_DIMENSION = 384;

	private ZooModel< String, float[] > model;
	private final Map< String, float[] > subtopicEmbeddings = new ConcurrentHashMap<>();
	private final SubtopicRepository subtopicRepository;

	public EmbeddingServiceImpl ( SubtopicRepository subtopicRepository ) {
		this.subtopicRepository = subtopicRepository;
	}

	@PostConstruct
	public void initializeModel () {
		try {
			log.info("Loading sentence-transformers model...");
			Criteria< String, float[] > criteria = Criteria.builder()
				.setTypes(String.class, float[].class)
				.optModelUrls("djl://ai.djl.huggingface.onnxruntime/sentence-transformers/all-MiniLM-L6-v2")
				.optEngine("OnnxRuntime")
				.optTranslator(new SentenceTransformerTranslator())
				.optProgress(new ai.djl.training.util.ProgressBar())
				.build();

			this.model = criteria.loadModel();
			log.info("Embedding model loaded successfully");
		} catch ( Throwable t ) {
			log.error("Failed to load embedding model. Semantic search will be disabled.", t);
			// handle model load failure gracefully, allowing app to start without semantic search
			this.model = null; // disable semantic search
		}
	}

	@Override
	public void initializeEmbeddings () {
		if ( model == null ) {
			log.warn("Embedding model not loaded, skipping embedding initialization");
			return;
		}

		log.info("Initializing embeddings for all subtopics...");
		List< Subtopic > subtopics = subtopicRepository.findAll();
		int count = 0;

		for ( Subtopic subtopic : subtopics ) {
			try {
				// combine title and content for richer semantic representation
				String textToEmbed = subtopic.getTitle() + " " + subtopic.getContent();

				float[] embedding = getEmbeddingForText(textToEmbed);
				subtopicEmbeddings.put(subtopic.getId(), embedding);
				count++;
			} catch ( Exception e ) {
				log.error("Failed to generate embedding for subtopic: {}", subtopic.getId(), e);
			}
		}

		log.info("Successfully generated embeddings for {} subtopics", count);
	}

	@Override
	public float[] getEmbeddingForText ( String text ) {
		if ( text == null || text.isBlank() ) {
			return new float[EMBEDDING_DIMENSION]; // return zero vector
		}

		if ( model == null ) {
			log.warn("Embedding model not loaded, returning zero vector");
			return new float[EMBEDDING_DIMENSION];
		}

		try ( Predictor< String, float[] > predictor = model.newPredictor() ) {
			return predictor.predict(text);
		} catch ( Exception e ) {
			log.error("Failed to generate embedding for text", e);
			// handling error gracefully by returning zero vector
			return new float[EMBEDDING_DIMENSION];
		}
	}

	@Override
	public double calculateSimilarity ( float[] vecA, float[] vecB ) {
		if ( vecA.length != vecB.length ) {
			throw new IllegalArgumentException("Vectors must have same dimensions");
		}

		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;

		for ( int i = 0; i < vecA.length; i++ ) {
			dotProduct += vecA[i] * vecB[i];
			normA += vecA[i] * vecA[i];
			normB += vecB[i] * vecB[i];
		}

		if ( normA == 0.0 || normB == 0.0 ) {
			return 0.0; // handle zero vectors
		}

		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}

	@Override
	public List< ScoredSubtopic > searchBySimilarity ( String query, int topK ) {
		float[] queryEmbedding = getEmbeddingForText(query);

		return subtopicEmbeddings.entrySet().parallelStream()
			.map(entry -> {
				String subtopicId = entry.getKey();
				float[] embedding = entry.getValue();
				double similarity = calculateSimilarity(queryEmbedding, embedding);
				return new ScoredSubtopic(subtopicId, similarity);
			})
			.sorted(Comparator.comparingDouble(ScoredSubtopic::score).reversed())
			.limit(topK)
			.toList();
	}
}
