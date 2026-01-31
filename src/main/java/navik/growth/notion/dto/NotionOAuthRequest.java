package navik.growth.notion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotionOAuthRequest {
	public record TokenRequest(
		@JsonProperty("grant_type") String grantType,
		String code,
		@JsonProperty("redirect_uri") String redirectUri
	) {
	}
}
