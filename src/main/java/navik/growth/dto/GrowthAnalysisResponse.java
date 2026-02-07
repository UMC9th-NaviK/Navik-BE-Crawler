package navik.growth.dto;

import lombok.Builder;

/**
 * 성장 기록 분석 응답 DTO
 */
@Builder
public record GrowthAnalysisResponse(
	String title,
	String summary,
	String feedback,
	Integer score
) {
}
