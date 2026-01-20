package navik.ai.client;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingClient {

	private final EmbeddingModel embeddingModel;

	public float[] embed(String text) {
		return embeddingModel.embed(text);
	}
}
