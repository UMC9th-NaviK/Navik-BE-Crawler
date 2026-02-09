package navik.growth.analysis.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class AnalysisRequest {

	public record GrowthAnalysisRequest(
			Long userId,
			Long jobId,
			Integer levelValue,
			Context context) {
		public record Context(
				String resumeText,
				java.util.List<GrowthLog> recentGrowthLogs,
				java.util.List<KpiDelta> recentKpiDeltas,
				String newContent) {
		}

		public record GrowthLog(
				Integer id,
				String type,
				String title,
				String content,
				Integer totalDelta,
				LocalDateTime createdAt) {
		}

		public record KpiDelta(
				Integer growthLogId,
				Integer kpiCardId,
				Integer delta) {
		}
	}
}
