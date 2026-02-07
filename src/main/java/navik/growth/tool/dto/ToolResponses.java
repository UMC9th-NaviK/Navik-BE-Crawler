package navik.growth.tool.dto;

public class ToolResponses {

	public record LevelCriteriaResult(
		Integer level,
		String scoringGuideline
	) {
	}
}
