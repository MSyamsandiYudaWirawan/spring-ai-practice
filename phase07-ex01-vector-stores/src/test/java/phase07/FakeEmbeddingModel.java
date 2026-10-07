package phase07;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic in-memory EmbeddingModel test double for Phase 07.
 * Produces unit-normalized vectors based on deterministic text hash seeds.
 * <p>
 * DO NOT MODIFY THIS FILE.
 */
public class FakeEmbeddingModel implements EmbeddingModel {

    private final int dimensions;
    private final AtomicInteger callCount = new AtomicInteger(0);

    public FakeEmbeddingModel() {
        this(16);
    }

    public FakeEmbeddingModel(int dimensions) {
        if (dimensions < 2) {
            throw new IllegalArgumentException("dimensions must be >= 2");
        }
        this.dimensions = dimensions;
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void resetCallCount() {
        callCount.set(0);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document != null ? document.getText() : "");
    }

    @Override
    public float[] embed(String text) {
        callCount.incrementAndGet();
        return computeNormalizedVector(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null) return List.of();
        List<float[]> list = new ArrayList<>(texts.size());
        for (String t : texts) {
            list.add(embed(t));
        }
        return list;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        callCount.incrementAndGet();
        List<String> instructions = request != null ? request.getInstructions() : List.of();
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            float[] vec = computeNormalizedVector(instructions.get(i));
            embeddings.add(new Embedding(vec, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private float[] computeNormalizedVector(String text) {
        float[] vec = new float[dimensions];
        if (text == null || text.isBlank()) {
            vec[0] = 1.0f;
            return vec;
        }

        String[] tokens = text.trim().toLowerCase().split("[^a-zA-Z0-9]+");
        int count = 0;
        for (String token : tokens) {
            if (token.isBlank()) continue;
            count++;
            Random rand = new Random((long) token.hashCode());
            for (int i = 0; i < dimensions; i++) {
                vec[i] += (rand.nextFloat() * 2.0f) - 1.0f;
            }
        }

        if (count == 0) {
            vec[0] = 1.0f;
            return vec;
        }

        float sumSq = 0.0f;
        for (int i = 0; i < dimensions; i++) {
            sumSq += vec[i] * vec[i];
        }

        float norm = (float) Math.sqrt(sumSq);
        if (norm > 1e-6f) {
            for (int i = 0; i < dimensions; i++) {
                vec[i] /= norm;
            }
        } else {
            vec[0] = 1.0f;
        }
        return vec;
    }
}
