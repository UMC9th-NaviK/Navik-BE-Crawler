package navik.growth.dto;

import lombok.Builder;

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
