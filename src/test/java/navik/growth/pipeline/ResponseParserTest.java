package navik.growth.pipeline;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import navik.growth.analysis.service.parser.ResponseParser;

class ResponseParserTest {
    private final ResponseParser parser = new ResponseParser(new ObjectMapper());

    @Test
    void createsTextOnlyDraftWithoutAnEmbeddingDependency() {
        var result = parser.parseResponse("""
            {"title":"T","content":"C","kpis":[{"kpiCardId":1,"delta":3}],"abilities":[" A ","A","B"]}
            """);
        assertThat(result.abilities()).containsExactly("A", "B");
        assertThat(result.kpis()).hasSize(1);
    }

    @Test
    void rejectsDuplicateKpiRatherThanApplyingItsScoreTwice() {
        assertThatThrownBy(() -> parser.parseResponse("""
            {"title":"T","content":"C","kpis":[{"kpiCardId":1,"delta":3},{"kpiCardId":1,"delta":4}],"abilities":[]}
            """)).hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFailedAnalysisAndMalformedAbility() {
        assertThatThrownBy(() -> parser.parseResponse("""
            {"title":"failed","content":"","kpis":[],"abilities":[]}
            """)).hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parseResponse("""
            {"title":"T","content":"C","kpis":[],"abilities":[{}]}
            """)).hasRootCauseInstanceOf(IllegalArgumentException.class);
    }
}
