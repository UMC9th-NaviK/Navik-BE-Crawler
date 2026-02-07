package navik.growth.analysis.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.growth.analysis.dto.AnalysisRequest.GrowthAnalysisRequest;
import navik.growth.analysis.dto.AnalysisResponse;
import navik.growth.analysis.service.parser.ResponseParser;
import navik.growth.analysis.service.prompt.PromptBuilder;
import navik.growth.analysis.service.util.ContentTypeHelper;
import navik.growth.analysis.service.util.ContentTypeHelper.ContentType;
import navik.growth.analysis.strategy.PersonaPromptLoader;

/**
 * AI 기반 성장 기록 분석 서비스
 *
 * Workflow:
 * 1. 요청 수신: 사용자 정보(JobId, Level)와 성장 기록(Content) 수신
 * 2. 전략 선택: JobId에 맞는 평가 페르소나(시스템 프롬프트) 로드
 * 3. AI 추론 및 도구 실행:
 *    - Step 1: AI가 JobId를 보고 retrieveKpiCards 호출 → DB에서 해당 직무의 10개 KPI 카드 정보 로드
 *    - Step 2: AI가 Level을 보고 retrieveLevelCriteria 호출 → 해당 레벨의 점수 산정 가이드라인 로드
 *    - Step 3: Content 타입(링크/텍스트)에 따른 신뢰도 가중치 적용
 *    - Step 4: recentKpiDeltas 확인 후 특정 kpiCard 빈도가 잦으면 해당 카드 점수 보정
 * 4. 결과 매핑: GrowthAnalysisResponse(title, content, 10개 kpiDeltas)로 변환하여 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthAnalysisService {

	private final ChatClient chatClient;
	private final PersonaPromptLoader personaPromptLoader;
	private final ContentTypeHelper contentTypeHelper;
	private final PromptBuilder promptBuilder;
	private final ResponseParser responseParser;

	/**
	 * 성장 기록 분석 수행
	 *
	 * @param request 분석 요청 (userId, jobId, levelValue, context)
	 * @return 분석 결과 (title, content, 10개 kpiDeltas)
	 */
	public AnalysisResponse.GrowthAnalysisResponse analyze(GrowthAnalysisRequest request) {
		// 1. 전략 선택: JobId에 맞는 평가 페르소나(시스템 프롬프트) 로드
		String systemPrompt = personaPromptLoader.load(request.jobId());

		// 2. 컨텐츠 타입 판별 및 프롬프트 구성
		ContentType contentType = contentTypeHelper.detectContentType(request.context().newContent());
		String userPrompt = promptBuilder.buildUserPrompt(request, contentType);

		// 3. AI 호출 (Spring AI Tool Calling)
		//    - 공통: retrieveKpiCards, retrieveLevelCriteria
		//    - 링크인 경우: fetchNotionPage 또는 fetchGitHubPR 추가
		List<String> toolNames = contentTypeHelper.buildToolNames(contentType);

		String responseContent = chatClient.prompt()
			.system(systemPrompt) // 페르소나
			.user(userPrompt) // 컨텍스트
			.options(ChatOptions.builder()
				.temperature(0.3)
				.build()
			)
			.toolNames(toolNames.toArray(String[]::new))
			.call()
			.content();

		// 4. 결과 매핑: JSON → GrowthAnalysisResponse
		AnalysisResponse.GrowthAnalysisResponse response = responseParser.parseResponse(responseContent);

		log.info("성장 기록 분석 완료 - userId: {}, jobId: {}, title: {}, kpiDeltas: {}개",
			request.userId(), request.jobId(), response.title(), response.kpis().size());

		return response;
	}
}