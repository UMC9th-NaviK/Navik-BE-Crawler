package navik.growth.notion.dto;

public record NotionWorkspaceToken(
	String accessToken,
	String workspaceId,
	String workspaceName,
	String workspaceIcon,
	String botId,
	long connectedAt
) {
}
