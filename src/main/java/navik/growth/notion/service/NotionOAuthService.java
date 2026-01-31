package navik.growth.notion.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;
import navik.growth.notion.config.NotionOAuthProperties;
import navik.growth.notion.dto.NotionOAuthRequest;
import navik.growth.notion.dto.NotionOAuthResponse;
import navik.growth.notion.dto.NotionOAuthResponse.TokenResponse;
import navik.growth.notion.dto.NotionOAuthResponse.WorkspaceInfo;
import navik.growth.notion.dto.NotionWorkspaceToken;
import navik.growth.notion.exception.NotionNotConnectedException;
import navik.growth.notion.repository.NotionTokenRepository;

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

		NotionOAuthResponse.TokenResponse response = webClient.post()
			.uri(NOTION_OAUTH_TOKEN_URL)
			.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(BodyInserters.fromValue(new NotionOAuthRequest.TokenRequest(
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
	 * TokenResponse 전체 정보로 워크스페이스 토큰 저장
	 */
	public void saveToken(String userId, TokenResponse tokenResponse) {
		NotionWorkspaceToken workspaceToken = new NotionWorkspaceToken(
			tokenResponse.accessToken(),
			tokenResponse.workspaceId(),
			tokenResponse.workspaceName(),
			tokenResponse.workspaceIcon(),
			tokenResponse.botId(),
			Instant.now().toEpochMilli()
		);
		tokenRepository.save(userId, workspaceToken);
	}

	/**
	 * 사용자의 모든 워크스페이스 토큰 조회
	 */
	public List<NotionWorkspaceToken> getAllWorkspaceTokens(String userId) {
		List<NotionWorkspaceToken> tokens = tokenRepository.findAllByUserId(userId);
		if (tokens.isEmpty()) {
			throw new NotionNotConnectedException(
				"Notion이 연동되지 않았습니다. 먼저 /api/notion/oauth/authorize?user_id=" + userId + " 로 연동해주세요."
			);
		}
		return tokens;
	}

	/**
	 * 연동된 워크스페이스 목록 반환
	 */
	public List<WorkspaceInfo> getConnectedWorkspaces(String userId) {
		return tokenRepository.findAllByUserId(userId).stream()
			.map(token -> new WorkspaceInfo(
				token.workspaceId(),
				token.workspaceName(),
				token.workspaceIcon(),
				token.connectedAt()
			))
			.toList();
	}

	/**
	 * 연동 여부 확인
	 */
	public boolean isConnected(String userId) {
		return tokenRepository.existsByUserId(userId);
	}

	/**
	 * 특정 워크스페이스 연동 해제
	 */
	public void disconnect(String userId, String workspaceId) {
		tokenRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);
	}

	/**
	 * 전체 워크스페이스 연동 해제
	 */
	public void disconnectAll(String userId) {
		tokenRepository.deleteAllByUserId(userId);
	}
}
