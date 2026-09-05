package navik.growth.analysis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import navik.ai.client.EmbeddingClient;
import navik.growth.analysis.dto.AnalysisDraft;
import navik.growth.analysis.dto.AnalysisResponse.GrowthAnalysisResponse;
import navik.growth.analysis.dto.AnalysisResponse.GrowthAnalysisResponse.Ability;

@Service
@RequiredArgsConstructor
public class GrowthEmbeddingService {
    private final EmbeddingClient client;

    public GrowthAnalysisResponse embed(AnalysisDraft draft, GrowthAnalysisResponse checkpoint,
        Consumer<GrowthAnalysisResponse> saveCheckpoint) {
        List<Ability> done = new ArrayList<>();
        if (checkpoint != null && checkpoint.abilities() != null) done.addAll(checkpoint.abilities());
        if (done.size() > draft.abilities().size()) throw new IllegalArgumentException("Invalid checkpoint");
        for (int i = 0; i < done.size(); i++) {
            if (!done.get(i).content().equals(draft.abilities().get(i))) throw new IllegalArgumentException("Input changed");
            validate(done.get(i).embedding());
        }
        for (int i = done.size(); i < draft.abilities().size(); i++) {
            String text = draft.abilities().get(i);
            float[] vector = client.embed(text);
            validate(vector);
            done.add(new Ability(text, vector));
            saveCheckpoint.accept(result(draft, done));
        }
        return result(draft, done);
    }

    private static GrowthAnalysisResponse result(AnalysisDraft draft, List<Ability> abilities) {
        return new GrowthAnalysisResponse(draft.title(), draft.content(), draft.kpis(), List.copyOf(abilities));
    }

    private static void validate(float[] vector) {
        if (vector == null || vector.length != 1536) throw new IllegalArgumentException("Invalid embedding dimension");
        for (float value : vector) if (!Float.isFinite(value)) throw new IllegalArgumentException("Invalid embedding value");
    }
}
