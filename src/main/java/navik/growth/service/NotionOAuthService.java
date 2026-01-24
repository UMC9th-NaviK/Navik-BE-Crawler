package navik.growth.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;
import navik.growth.config.NotionOAuthProperties;
import navik.growth.repository.NotionTokenRepository;

@Slf4j
@Service
public class NotionOAuthService {

	private static final String NOTION_OAUTH_AUTHORIZE_URL = "https://api.notion.com/v1/oauth/authorize";
	private static final String NOTION_OAUTH_TOKEN_URL = "https://api.notion.com/v1/oauth/token";

	private final NotionOAuthProperties properties;
	private final NotionTokenRepository tokenRepository;
	private final WebClient webClient;

	public NotionOAuthService(
		NotionOAuthProperties properties,
		NotionTokenRepository tokenRepository,
		@Qualifier("notionWebClient") WebClient webClient
	) {
		this.properties = properties;
		this.tokenRepository = tokenRepository;
		this.webClient = webClient;
	}

	/**
	 * Notion OAuth 인증 URL 생성
	 * state에 userId를 포함하여 콜백 시 식별
	 */
	public String buildAuthorizationUrl(String userId) {
		return UriComponentsBuilder.fromHttpUrl(NOTION_OAUTH_AUTHORIZE_URL)
			.queryParam("client_id", properties.oauth().clientId())
			.queryParam("redirect_uri", properties.oauth().redirectUri())
			.queryParam("response_type", "code")
			.queryParam("owner", "user")
			.queryParam("state", userId)
			.build()
			.toUriString();
	}

	/**
	 * Authorization Code를 Access Token으로 교환
	 */
	public TokenResponse exchangeCodeForToken(String code) {
		log.info("Notion OAuth 토큰 교환 시작");

		// Basic Auth: base64(client_id:client_secret)
		String credentials = properties.oauth().clientId() + ":" + properties.oauth().clientSecret();
		String encodedCredentials = Base64.getEncoder()
			.encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

		TokenResponse response = webClient.post()
			.uri(NOTION_OAUTH_TOKEN_URL)
			.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(BodyInserters.fromValue(new TokenRequest(
				"authorization_code",
				code,
				properties.oauth().redirectUri()
			)))
			.retrieve()
			.bodyToMono(TokenResponse.class)
			.block();

		log.info("Notion OAuth 토큰 교환 완료: workspaceName={}",
			response != null ? response.workspaceName() : "null");

		return response;
	}

	/**
	 * 토큰 저장
	 */
	public void saveToken(String userId, String accessToken) {
		tokenRepository.save(userId, accessToken);
	}

	/**
	 * 사용자 토큰 조회
	 */
	public String getAccessToken(String userId) {
		return tokenRepository.findByUserId(userId)
			.orElseThrow(() -> new NotionNotConnectedException(
				"Notion이 연동되지 않았습니다. 먼저 /api/notion/oauth/authorize?user_id=" + userId + " 로 연동해주세요."
			));
	}

	/**
	 * 연동 여부 확인
	 */
	public boolean isConnected(String userId) {
		return tokenRepository.existsByUserId(userId);
	}

	/**
	 * 연동 해제
	 */
	public void disconnect(String userId) {
		tokenRepository.deleteByUserId(userId);
	}

	// Request/Response DTOs
	public record TokenRequest(
		@JsonProperty("grant_type") String grantType,
		String code,
		@JsonProperty("redirect_uri") String redirectUri
	) {
	}

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

	public static class NotionNotConnectedException extends RuntimeException {
		public NotionNotConnectedException(String message) {
			super(message);
		}
	}
}
