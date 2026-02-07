package com.courseplatform.service.impl;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

public class SentenceTransformerTranslator implements Translator<String, float[]> {

	private HuggingFaceTokenizer tokenizer;

	@Override
	public Batchifier getBatchifier() {
		return Batchifier.STACK;
	}

	@Override
	public void prepare(TranslatorContext ctx) throws Exception {
		this.tokenizer = HuggingFaceTokenizer.newInstance(ctx.getModel().getModelPath());
	}

	@Override
	@SuppressWarnings("java:S2095") // NDArrays are managed by DJL framework via returned NDList
	public NDList processInput(TranslatorContext ctx, String input) {
		Encoding encoding = tokenizer.encode(input);
		NDManager manager = ctx.getNDManager();

		long[] inputIds = encoding.getIds();
		long[] attentionMask = encoding.getAttentionMask();

		// we wont be adding batch dimension - DJL Predictor handles that automatically
		NDArray inputIdsArray = manager.create(inputIds);
		NDArray attentionMaskArray = manager.create(attentionMask);

		return new NDList(inputIdsArray, attentionMaskArray);
	}

	@Override
	public float[] processOutput(TranslatorContext ctx, NDList list) {
		// the model returns the last_hidden_state
		// Shape: [sequence_length, hidden_size] (batch dimension handled by DJL)
		NDArray embeddings = list.getFirst();

		// perform mean pooling across tokens (dim=0) to get sentence embedding
		// Shape after mean: [hidden_size]
		NDArray meanPooled = embeddings.mean(new int[]{0});

		// convert to float array
		// Shape: [hidden_size] (384 for all-MiniLM-L6-v2)
		return meanPooled.toFloatArray();
	}
}
