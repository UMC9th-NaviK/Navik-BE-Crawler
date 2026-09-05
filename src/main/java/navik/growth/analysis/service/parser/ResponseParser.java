package navik.growth.analysis.service.parser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.growth.analysis.dto.AnalysisDraft;
import navik.growth.analysis.dto.AnalysisResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseParser {

    private final ObjectMapper objectMapper;


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
	public AnalysisDraft parseResponse(String content) {
		try {
			String json = extractJsonFromContent(content);
			JsonNode node = objectMapper.readTree(json);

			String title = getTextValue(node, "title", "제목 없음");
			String responseContent = getTextValue(node, "content", "");

			List<AnalysisResponse.GrowthAnalysisResponse.KpiDelta> kpis = parseKpiDeltas(node);
			List<String> abilities = parseAbilities(node);

            if (title.isBlank() || responseContent.isBlank()) {
                throw new IllegalArgumentException("Analysis has no usable title or content");
            }
            return new AnalysisDraft(title, responseContent, kpis, abilities);

		} catch (Exception e) {
			log.error("AI 응답 파싱 실패: error={}", e.getClass().getSimpleName());
			throw new RuntimeException("AI 응답 파싱 실패", e);
		}
	}

	private List<AnalysisResponse.GrowthAnalysisResponse.KpiDelta> parseKpiDeltas(JsonNode node) {
		List<AnalysisResponse.GrowthAnalysisResponse.KpiDelta> kpis = new ArrayList<>();
		JsonNode kpisNode = node.get("kpis");

		if (kpisNode != null && kpisNode.isArray()) {
			for (JsonNode kpiNode : kpisNode) {
                if (!kpiNode.path("kpiCardId").isIntegralNumber() || !kpiNode.path("delta").isIntegralNumber()) {
                    throw new IllegalArgumentException("Invalid KPI numbers");
                }
                long kpiCardId = kpiNode.get("kpiCardId").asLong();
                int delta = kpiNode.get("delta").asInt();
                if (kpiCardId <= 0 || delta < 0 || delta > 15 || kpis.stream().anyMatch(k -> k.kpiCardId() == kpiCardId)) {
                    throw new IllegalArgumentException("Invalid or repeated KPI");
                }
				kpis.add(new AnalysisResponse.GrowthAnalysisResponse.KpiDelta(kpiCardId, delta));
			}
		}

		return kpis;
	}

	private static final int MAX_ABILITIES = 10;

	private List<String> parseAbilities(JsonNode node) {
		List<String> abilities = new ArrayList<>();
		JsonNode abilitiesNode = node.get("abilities");

		if (abilitiesNode != null && abilitiesNode.isArray()) {
			for (JsonNode abilityNode : abilitiesNode) {
				if (abilities.size() >= MAX_ABILITIES) {
					break;
				}
                if (!abilityNode.isTextual() || abilityNode.asText().isBlank()) {
                    throw new IllegalArgumentException("Ability must be a nonblank string");
                }
                String abilityContent = abilityNode.asText().trim();
                if (!abilities.contains(abilityContent)) abilities.add(abilityContent);
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
