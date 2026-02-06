package navik.growth.notion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notion")
public record NotionOAuthProperties(
	String apiVersion,
	OAuth oauth
) {
	public record OAuth(
		String clientId,
		String clientSecret,
		String redirectUri
	) {
	}
}
