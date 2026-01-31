package navik.growth.notion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotionOAuthResponse {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TokenResponse(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("token_type") String tokenType,
		@JsonProperty("bot_id") String botId,
		@JsonProperty("workspace_id") String workspaceId,
		@JsonProperty("workspace_name") String workspaceName,
		@JsonProperty("workspace_icon") String workspaceIcon,
		Owner owner
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Owner(
		String type,
		User user
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record User(
		String id,
		String name,
		@JsonProperty("avatar_url") String avatarUrl
	) {
	}

	public record CallbackResponse(
		boolean success,
		String message,
		String userId,
		String workspaceName
	) {
	}

	public record StatusResponse(
		String userId,
		boolean connected
	) {
	}

	public record DisconnectResponse(
		String userId,
		String message
	) {
	}
}
