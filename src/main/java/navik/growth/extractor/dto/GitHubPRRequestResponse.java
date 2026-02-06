package navik.growth.extractor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GitHubPRRequestResponse {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GitHubPR(
		String title,
		String body,
		String state,
		User user,
		@JsonProperty("created_at") String createdAt,
		int additions,
		int deletions,
		int commits,
		@JsonProperty("changed_files") int changedFiles,
		Branch base,
		Branch head
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record User(String login) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Branch(String ref) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GitHubPRFile(
		String filename,
		String status,
		int additions,
		int deletions,
		String patch
	) {
	}
}
