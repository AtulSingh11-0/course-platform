package com.courseplatform.service;

import java.util.List;

public interface EmbeddingService {
	void initializeEmbeddings();
	float[] getEmbeddingForText(String text);
	double calculateSimilarity(float[] vecA, float[] vecB);
	List< ScoredSubtopic > searchBySimilarity ( String query, int topK );

	record ScoredSubtopic (
		String subtopicId,
		double score
	) {}
}
