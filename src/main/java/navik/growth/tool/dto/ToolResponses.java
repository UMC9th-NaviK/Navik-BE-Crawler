package navik.growth.tool.dto;

import java.util.List;

public class ToolResponses {

	public record KpiCardsResult(
		List<KpiCard> cards
	) {
		public record KpiCard(
			Long id,
			String name,
			String description,
			String category
		) {
		}
	}

	public record LevelCriteriaResult(
		Integer level,
		String scoringGuideline
	) {
	}
}
