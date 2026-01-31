package navik.growth.tool.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class ToolRequests {
	public record NotionPageRequest(
		@JsonProperty(required = true)
		@JsonPropertyDescription("사용자 식별자 (Notion OAuth 토큰 조회용)")
		String userId,

		@JsonProperty(required = true)
		@JsonPropertyDescription("노션 페이지 URL (예: https://notion.so/xxxxx 또는 https://xxx.notion.site/xxxxx)")
		String url
	) {
	}

	public record GitHubPRRequest(
		@JsonProperty(required = true)
		@JsonPropertyDescription("GitHub Pull Request URL (예: https://github.com/owner/repo/pull/123)")
		String url
	) {
	}
}
