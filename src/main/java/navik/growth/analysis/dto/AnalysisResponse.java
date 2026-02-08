package navik.growth.analysis.dto;

import java.util.List;

import lombok.Builder;

public class AnalysisResponse {

	/**
	 * 성장 기록 분석 응답 DTO
	 */
	@Builder
	public record GrowthAnalysisResponse(
		String title,
		String content,
		List<KpiDelta> kpis,
		List<Ability> abilities // 추가됨
	) {
		public record KpiDelta(
			Long kpiCardId,
			Integer delta
		) {
		}
		public record Ability(
			String content,
			float[] embedding
		) {
		}
	}
}
