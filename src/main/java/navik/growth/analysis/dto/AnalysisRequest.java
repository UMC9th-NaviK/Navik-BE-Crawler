package navik.growth.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class AnalysisRequest {

	/**
	 * 성장 기록 분석 요청 DTO
	 */
	public record GrowthAnalysisRequest(
		@NotBlank(message = "사용자 ID는 필수입니다.")
		String userId,

		@NotBlank(message = "분석할 URL은 필수입니다.")
		String sourceUrl,

		@NotNull(message = "직무 컨텍스트는 필수입니다.")
		JobContext jobContext
	) {
	}

	/**
	 * 직무 및 KPI 정보
	 */
	@Builder
	public record JobContext(
		String jobName,
		String kpiCardName,
		String strongTitle,
		String weakTitle
	) {
	}
}
