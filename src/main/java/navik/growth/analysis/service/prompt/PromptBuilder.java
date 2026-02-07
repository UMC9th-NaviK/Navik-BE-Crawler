package navik.growth.analysis.service.prompt;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import navik.ai.util.PromptLoader;
import navik.growth.analysis.dto.AnalysisRequest.GrowthAnalysisRequest;
import navik.growth.analysis.service.util.ContentTypeHelper.ContentType;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

	private final PromptLoader promptLoader;

	private static final String USER_PROMPT_TEMPLATE_PATH = "classpath:prompts/growth/templates/user-prompt-template.txt";

    /**
	 * 사용자 프롬프트 구성
	 * 컨텐츠 타입(링크/텍스트)에 따라 분기하여 프롬프트 생성
	 */
	public String buildUserPrompt(GrowthAnalysisRequest request, ContentType contentType) {

		// 1. [Pre-calculation] KPI 포화도 맵 생성 (List -> Map<KpiId, Count>)
		Map<Integer, Long> saturationMap = request.context().recentKpiDeltas().stream()
			.collect(Collectors.groupingBy(
				GrowthAnalysisRequest.KpiDelta::kpiCardId,
				Collectors.counting()
			));

		// 2. 컨텐츠 타입별 분석 대상 섹션 생성
		String contentSection = buildContentSection(request, contentType);

		// 3. 템플릿 로드 및 변수 치환
		String template = promptLoader.loadPrompt(USER_PROMPT_TEMPLATE_PATH);

		return template
			.replace("{CONTENT_SECTION}", contentSection)
			.replace("{LEVEL_VALUE}", String.valueOf(request.levelValue()))
			.replace("{RESUME_TEXT}", request.context().resumeText())
			.replace("{SATURATION_MAP}", saturationMap.toString())
			.replace("{CONTENT_INSTRUCTION}", buildContentInstruction(contentType));
	}

	/**
	 * 컨텐츠 타입별 분석 대상 섹션 생성
	 * - 링크: Tool로 컨텐츠를 추출하도록 URL과 userId 전달
	 * - 텍스트: 본문을 직접 프롬프트에 포함
	 */
	private String buildContentSection(GrowthAnalysisRequest request, ContentType contentType) {
		String newContent = request.context().newContent();
		return switch (contentType) {
			case NOTION_LINK -> String.format("""
                TYPE: [Verified Link - Notion] (Higher reliability score allowed)
                URL: %s
                User ID: %s (Required for 'fetchNotionPage' tool call)
                * You MUST call 'fetchNotionPage' with the above userId and URL to retrieve the content before analysis.""",
				newContent, request.userId());

			case GITHUB_PR_LINK -> String.format("""
                TYPE: [Verified Link - GitHub PR] (Higher reliability score allowed)
                URL: %s
                * You MUST call 'fetchGitHubPR' with the above URL to retrieve the PR content before analysis.""",
				newContent);
			case TEXT -> String.format("""
                TYPE: [Self-Reported Text] (Strict evaluation required)
                Content: \"%s\"
                * Analyze the text content directly. No external content retrieval needed.""",
				newContent.replace("\"", "'" ));
		};
	}

	/**
	 * 컨텐츠 타입별 AI 지시 3번 항목 생성
	 */
	private String buildContentInstruction(ContentType contentType) {
		return switch (contentType) {
			case NOTION_LINK -> "3. Call 'fetchNotionPage' with the User ID and URL, then analyze the retrieved content.";
			case GITHUB_PR_LINK -> "3. Call 'fetchGitHubPR' with the URL, then analyze the retrieved PR content.";
			case TEXT -> "3. Analyze the provided text content directly.";
		};
	}
}
