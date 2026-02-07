package navik.growth.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.growth.dto.GrowthAnalysisRequest;
import navik.growth.dto.GrowthAnalysisResponse;
import navik.growth.dto.JobContext;

/**
 * AI 기반 성장 기록 분석 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthAnalysisService {

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 성장 기록 분석 수행
	 *
	 * @param request 분석 요청 (URL + 직무 컨텍스트)
	 * @return 분석 결과 (제목, 요약, 피드백, 점수)
	 */
	public GrowthAnalysisResponse analyze(GrowthAnalysisRequest request) {
		log.info("성장 기록 분석 시작 - URL: {}, 직무: {}",
			request.sourceUrl(), request.jobContext().jobName());

		// 1. 시스템 프롬프트 생성
		String systemPromptText = buildSystemPrompt(request.jobContext());

		// 2. 사용자 프롬프트 생성
		String userPromptText = buildUserPrompt(request);

		// 3. AI API 호출 (Tool 포함)
		SystemMessage systemMessage = SystemMessage.builder()
			.text(systemPromptText)
			.build();

		UserMessage userMessage = UserMessage.builder()
			.text(userPromptText)
			.build();

		String responseContent = chatClient.prompt()
			.messages(systemMessage, userMessage)
			.options(ChatOptions.builder()
				.temperature(0.3)
				.build()
			)
			.toolNames("fetchNotionPage", "fetchGitHubPR")
			.call()
			.content();

		// 4. 응답 파싱
		GrowthAnalysisResponse response = parseResponse(responseContent);

		log.info("성장 기록 분석 완료 - 제목: {}, 점수: {}", response.title(), response.score());

		return response;
	}

	private String buildSystemPrompt(JobContext jobContext) {
		return """
			당신은 %s 직무의 성장 분석 전문가입니다.

			분석 대상 KPI: %s
			- 강점: %s
			- 약점: %s

			사용자가 제공한 URL의 학습 기록을 분석하여 다음을 제공해주세요:

			1. 핵심 내용 요약 (500자 이내)
			   - 학습한 기술/개념
			   - 수행한 작업
			   - 주요 성과

			2. KPI 관점의 구체적 피드백 (1000자 이내)
			   - 강점 관점에서 잘한 점
			   - 약점 관점에서 보완할 점
			   - 다음 학습 방향 제안

			3. 학습 성과 점수 (0-20점)
			   - 0-5점: 기초 개념 학습
			   - 6-10점: 실습 및 적용
			   - 11-15점: 심화 학습 및 응용
			   - 16-20점: 독창적 문제 해결 또는 프로덕션 적용

			응답 형식은 반드시 다음 JSON 구조를 따라주세요:
			```json
			{
			  "title": "추출된 제목",
			  "summary": "요약 내용",
			  "feedback": "피드백 내용",
			  "score": 15
			}
			```

			**중요:**
			- URL 내용을 확인하려면 제공된 Tool을 사용하세요:
			  - 노션 페이지: fetchNotionPage
			  - GitHub PR: fetchGitHubPR
			- Tool을 먼저 호출하여 컨텐츠를 가져온 후 분석하세요.
			- 반드시 유효한 JSON만 반환하세요.
			""".formatted(
			jobContext.jobName(),
			jobContext.kpiCardName(),
			jobContext.strongTitle() != null ? jobContext.strongTitle() : "미지정",
			jobContext.weakTitle() != null ? jobContext.weakTitle() : "미지정"
		);
	}

	private String buildUserPrompt(GrowthAnalysisRequest request) {
		return """
			다음 URL의 학습 기록을 분석해주세요:
			%s

			사용자 ID: %s
			직무: %s
			KPI 카드: %s

			위 URL의 내용을 먼저 Tool을 사용해 가져온 후,
			'%s' KPI 카드 관점에서 분석해주세요.

			**중요: fetchNotionPage Tool 호출 시 반드시 userId에 "%s"를 사용하세요.**
			""".formatted(
			request.sourceUrl(),
			request.userId(),
			request.jobContext().jobName(),
			request.jobContext().kpiCardName(),
			request.jobContext().kpiCardName(),
			request.userId()
		);
	}

	private GrowthAnalysisResponse parseResponse(String content) {
		try {
			// JSON 블록 추출 (```json ... ``` 제거)
			String json = extractJsonFromContent(content);

			JsonNode node = objectMapper.readTree(json);

			return GrowthAnalysisResponse.builder()
				.title(getTextValue(node, "title", "제목 없음"))
				.summary(getTextValue(node, "summary", "요약 없음"))
				.feedback(getTextValue(node, "feedback", "피드백 없음"))
				.score(getIntValue(node, "score", 0))
				.build();

		} catch (Exception e) {
			log.error("AI 응답 파싱 실패: {}", content, e);
			throw new RuntimeException("AI 응답 파싱 실패", e);
		}
	}

	private String extractJsonFromContent(String content) {
		String json = content.trim();

		// ```json ... ``` 형식 처리
		if (json.contains("```json")) {
			int start = json.indexOf("```json") + 7;
			int end = json.lastIndexOf("```");
			if (end > start) {
				json = json.substring(start, end).trim();
			}
		}
		// ``` ... ``` 형식 처리
		else if (json.contains("```")) {
			int start = json.indexOf("```") + 3;
			int end = json.lastIndexOf("```");
			if (end > start) {
				json = json.substring(start, end).trim();
			}
		}

		// { ... } 블록만 추출
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

	private int getIntValue(JsonNode node, String field, int defaultValue) {
		JsonNode fieldNode = node.get(field);
		if (fieldNode != null && !fieldNode.isNull()) {
			return fieldNode.asInt();
		}
		return defaultValue;
	}
}
