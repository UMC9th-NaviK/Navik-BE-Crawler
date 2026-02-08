package navik.growth.analysis.service.parser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.ai.client.EmbeddingClient;
import navik.growth.analysis.dto.AnalysisResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseParser {

    private final ObjectMapper objectMapper;
    private final EmbeddingClient embeddingClient;

    /**
	 * AI 응답 JSON을 GrowthAnalysisResponse로 파싱
	 * 기대 JSON 구조:
	 * {
	 *   "title": "...",
	 *   "content": "...",
	 *   "kpis": [
	 *     {"kpiCardId": 1, "delta": 5},
	 *     {"kpiCardId": 2, "delta": 3},
	 *     ... (총 10개)
	 *   ]
	 * }
	 */
	public AnalysisResponse.GrowthAnalysisResponse parseResponse(String content) {
		try {
			String json = extractJsonFromContent(content);
			JsonNode node = objectMapper.readTree(json);

			String title = getTextValue(node, "title", "제목 없음");
			String responseContent = getTextValue(node, "content", "");

			List<AnalysisResponse.GrowthAnalysisResponse.KpiDelta> kpis = parseKpiDeltas(node);
			List<AnalysisResponse.GrowthAnalysisResponse.Ability> abilities = parseAbilities(node);

			return AnalysisResponse.GrowthAnalysisResponse.builder()
				.title(title)
				.content(responseContent)
				.kpis(kpis)
				.abilities(abilities)
				.build();

		} catch (Exception e) {
			log.error("AI 응답 파싱 실패: {}", content, e);
			throw new RuntimeException("AI 응답 파싱 실패", e);
		}
	}

	private List<AnalysisResponse.GrowthAnalysisResponse.KpiDelta> parseKpiDeltas(JsonNode node) {
		List<AnalysisResponse.GrowthAnalysisResponse.KpiDelta> kpis = new ArrayList<>();
		JsonNode kpisNode = node.get("kpis");

		if (kpisNode != null && kpisNode.isArray()) {
			for (JsonNode kpiNode : kpisNode) {
				long kpiCardId = kpiNode.get("kpiCardId").asLong();
				int delta = kpiNode.get("delta").asInt();
				kpis.add(new AnalysisResponse.GrowthAnalysisResponse.KpiDelta(kpiCardId, delta));
			}
		}

		return kpis;
	}

	private static final int MAX_ABILITIES = 10;

	private List<AnalysisResponse.GrowthAnalysisResponse.Ability> parseAbilities(JsonNode node) {
		List<AnalysisResponse.GrowthAnalysisResponse.Ability> abilities = new ArrayList<>();
		JsonNode abilitiesNode = node.get("abilities");

		if (abilitiesNode != null && abilitiesNode.isArray()) {
			for (JsonNode abilityNode : abilitiesNode) {
				if (abilities.size() >= MAX_ABILITIES) {
					break;
				}
				String abilityContent = abilityNode.asText();
				abilities.add(new AnalysisResponse.GrowthAnalysisResponse.Ability(abilityContent, embeddingClient.embed(abilityContent)));
			}
		}

		return abilities;
	}

	private String extractJsonFromContent(String content) {
		String json = content.trim();

		if (json.contains("```json")) {
			int start = json.indexOf("```json") + 7;
			int end = json.lastIndexOf("```");
			if (end > start) {
				json = json.substring(start, end).trim();
			}
		} else if (json.contains("```")) {
			int start = json.indexOf("```") + 3;
			int end = json.lastIndexOf("```");
			if (end > start) {
				json = json.substring(start, end).trim();
			}
		}

		if (!json.startsWith("{")) {
			int start = json.indexOf("{");
			int end = json.lastIndexOf("}");
			if (start >= 0 && end > start) {
				json = json.substring(start, end + 1);
			}
		}

		return json;
	}

	private String getTextValue(JsonNode node, String field, String defaultValue) {
		JsonNode fieldNode = node.get(field);
		if (fieldNode != null && !fieldNode.isNull()) {
			return fieldNode.asText();
		}
		return defaultValue;
	}
}
