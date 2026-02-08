package navik.growth.config;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import navik.growth.extractor.GitHubPRExtractor;
import navik.growth.extractor.NotionPageExtractor;
import navik.growth.tool.dto.ToolRequests.GitHubPRRequest;
import navik.growth.tool.dto.ToolRequests.LevelCriteriaRequest;
import navik.growth.tool.dto.ToolRequests.NotionPageRequest;
import navik.growth.tool.service.LevelCriteriaService;

/**
 * Spring AI Function Calling을 위한 Tool 정의
 */
@Configuration
public class GrowthAnalysisToolConfig {

	/**
	 * 노션 페이지의 전체 컨텐츠를 추출하는 Tool
	 * 사용자별 OAuth 토큰으로 접근
	 */
	@Bean
	@Description("노션 페이지의 전체 컨텐츠를 추출합니다. 사용자가 Notion OAuth로 연동한 페이지만 접근 가능합니다. userId와 노션 URL을 입력받아 마크다운 형식으로 내용을 반환합니다.")
	public Function<NotionPageRequest, String> fetchNotionPage(NotionPageExtractor extractor) {
		return request -> {
			try {
				return extractor.extractPage(Long.parseLong(request.userId()), request.url());
			} catch (Exception e) {
				// 에러 로그 추가
				org.slf4j.LoggerFactory.getLogger(GrowthAnalysisToolConfig.class)
						.error("Notion Page Extraction Failed. userId={}, url={}, error={}",
								request.userId(), request.url(), e.getMessage(), e);
				return "Error: 노션 페이지를 가져오는데 실패했습니다. Notion 연동 여부를 확인하세요. " + e.getMessage();
			}
		};
	}

	/**
	 * GitHub Public PR의 변경사항과 설명을 추출하는 Tool
	 */
	@Bean
	@Description("GitHub Public PR의 변경사항과 설명을 추출합니다. GitHub PR URL을 입력받아 PR 정보를 마크다운 형식으로 반환합니다.")
	public Function<GitHubPRRequest, String> fetchGitHubPR(GitHubPRExtractor extractor) {
		return request -> {
			try {
				return extractor.extractPublicPR(request.url());
			} catch (Exception e) {
				return "Error: GitHub PR을 가져오는데 실패했습니다. " + e.getMessage();
			}
		};
	}

	/**
	 * 레벨 값으로 해당 레벨의 점수 산정 가이드라인을 조회하는 Tool
	 * AI가 Level을 보고 호출하여 점수 산정 기준을 로드
	 */
	@Bean
	@Description("입력된 레벨(1~10)에 해당하는 독립적인 평가 프롬프트를 로드합니다. " +
		"각 프롬프트는 해당 레벨의 유저가 갖춰야 할 핵심 역량, 설계 판단력, " +
		"그리고 이전 레벨을 포함하는 누적된 기술 수준을 평가하기 위한 AI 전용 가이드라인을 포함합니다.")
	public Function<LevelCriteriaRequest, String> retrieveLevelCriteria(LevelCriteriaService levelCriteriaService) {
		return request -> {
			try {
				return levelCriteriaService.findCriteria(request.jobId(), request.levelValue());
			} catch (Exception e) {
				return "Error: 레벨 가이드라인 조회에 실패했습니다. " + e.getMessage();
			}
		};
	}
}
