package navik.growth.config;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import navik.growth.extractor.GitHubPRExtractor;
import navik.growth.extractor.NotionPageExtractor;
import navik.growth.tool.dto.ToolRequests.GitHubPRRequest;
import navik.growth.tool.dto.ToolRequests.KpiRetrievalRequest;
import navik.growth.tool.dto.ToolRequests.JobScopeRequest;
import navik.growth.tool.dto.ToolRequests.LevelCriteriaRequest;
import navik.growth.tool.dto.ToolRequests.NotionPageRequest;
import navik.growth.tool.service.JobScopeService;
import navik.growth.tool.service.KpiCardService;
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
	@Description("노션 페이지의 전체 컨텐츠를 추출합니다. 사용자가 Notion OAuth로 연동한 페이지만 접근 가능하며, 실패 시 'Error: '로 시작하는 메시지를 반환합니다. userId와 노션 URL을 입력받아 마크다운 형식으로 내용을 반환합니다.")
	public Function<NotionPageRequest, String> fetchNotionPage(NotionPageExtractor extractor) {
		return request -> {
			try {
				return extractor.extractPage(request.userId(), request.url());
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
	@Description("GitHub Public PR의 변경사항과 설명을 추출합니다. 실패 시 'Error: '로 시작하는 메시지를 반환합니다. GitHub PR URL을 입력받아 PR 정보를 마크다운 형식으로 반환합니다.")
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
	 * 직무 ID로 해당 직무의 10개 KPI 카드 정보를 조회하는 Tool
	 * AI가 JobId를 보고 호출하여 DB에서 해당 직무의 KPI 카드 정보를 로드
	 */
	@Bean
	@Description("직무 ID로 해당 직무의 10개 KPI 카드 정보를 조회합니다. 각 카드의 id, name, description, category를 포함한 텍스트를 반환합니다.")
	public Function<KpiRetrievalRequest, String> retrieveKpiCards(KpiCardService kpiCardService) {
		return request -> {
			try {
				return kpiCardService.findKpiCardsByJobId(request.jobId());
			} catch (Exception e) {
				return "Error: KPI 카드 조회에 실패했습니다. " + e.getMessage();
			}
		};
	}

	/**
	 * 레벨 값으로 해당 레벨의 점수 산정 가이드라인을 조회하는 Tool
	 * AI가 Level을 보고 호출하여 점수 산정 기준을 로드
	 */
	@Bean
	@Description("사용자 레벨 값으로 해당 레벨의 점수 산정 가이드라인을 조회합니다. 레벨에 맞는 평가 기준과 점수 범위를 텍스트로 반환합니다.")
	public Function<LevelCriteriaRequest, String> retrieveLevelCriteria(LevelCriteriaService levelCriteriaService) {
		return request -> {
			try {
				return levelCriteriaService.findCriteria(request.jobId(), request.levelValue());
			} catch (Exception e) {
				return "Error: 레벨 가이드라인 조회에 실패했습니다. " + e.getMessage();
			}
		};
	}

	/**
	 * 직무 ID로 해당 직무의 핵심 책임과 명시적 제외 항목을 조회하는 Tool
	 * AI가 직무와 무관한 활동에 점수를 부여하지 않도록 판단 기준을 제공
	 */
	@Bean
	@Description("직무 ID로 해당 직무의 핵심 책임(coreResponsibilities)과 명시적 제외 항목(explicitlyExcluded)을 조회합니다. AI가 직무와 무관한 활동에 점수를 부여하지 않도록 판단 기준을 제공합니다.")
	public Function<JobScopeRequest, String> retrieveJobScope(JobScopeService jobScopeService) {
		return request -> {
			try {
				return jobScopeService.findJobScopeByJobId(request.jobId());
			} catch (Exception e) {
				return "Error: 직무 범위 조회에 실패했습니다. " + e.getMessage();
			}
		};
	}
}
