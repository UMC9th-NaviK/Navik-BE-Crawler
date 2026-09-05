package navik.growth.pipeline;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import navik.ai.client.EmbeddingClient;
import navik.growth.analysis.dto.AnalysisDraft;
import navik.growth.analysis.dto.AnalysisResponse.GrowthAnalysisResponse;
import navik.growth.analysis.service.GrowthEmbeddingService;

class GrowthEmbeddingServiceTest {
    @Test
    void resumesAfterPartialFailureWithoutEmbeddingSuccessfulTextAgain() {
        var client = mock(EmbeddingClient.class);
        var service = new GrowthEmbeddingService(client);
        var draft = new AnalysisDraft("title", "content", List.of(), List.of("first", "second"));
        when(client.embed("first")).thenReturn(new float[1536]);
        when(client.embed("second")).thenThrow(new RuntimeException("temporary")).thenReturn(new float[1536]);
        var saved = new AtomicReference<GrowthAnalysisResponse>();
        assertThatThrownBy(() -> service.embed(draft, null, saved::set)).isInstanceOf(RuntimeException.class);
        assertThat(saved.get().abilities()).hasSize(1);
        var result = service.embed(draft, saved.get(), saved::set);
        assertThat(result.abilities()).hasSize(2);
        verify(client, times(1)).embed("first");
        verify(client, times(2)).embed("second");
    }

    @Test
    void invalidVectorIsNeverCheckpointed() {
        var client = mock(EmbeddingClient.class);
        when(client.embed(anyString())).thenReturn(new float[3]);
        var saved = new AtomicReference<GrowthAnalysisResponse>();
        assertThatThrownBy(() -> new GrowthEmbeddingService(client).embed(
            new AnalysisDraft("t", "c", List.of(), List.of("ability")), null, saved::set))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(saved).hasNullValue();
    }

    @Test
    void changedDraftCannotReuseAnOlderCheckpoint() {
        var client = mock(EmbeddingClient.class);
        var checkpoint = new GrowthAnalysisResponse("t", "c", List.of(),
            List.of(new GrowthAnalysisResponse.Ability("old", new float[1536])));
        assertThatThrownBy(() -> new GrowthEmbeddingService(client).embed(
            new AnalysisDraft("t", "c", List.of(), List.of("new")), checkpoint, ignored -> {}))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }
}
