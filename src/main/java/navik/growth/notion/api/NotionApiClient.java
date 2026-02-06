package navik.growth.notion.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import navik.growth.extractor.dto.NotionApiResponses.Block;
import navik.growth.extractor.dto.NotionApiResponses.BlockList;
import navik.growth.extractor.dto.NotionApiResponses.Page;
import navik.growth.notion.config.NotionOAuthProperties;
import navik.growth.notion.exception.NotionApiException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class NotionApiClient {

	private static final String NOTION_API_BASE_URL = "https://api.notion.com/v1";

	private final WebClient baseWebClient;
	private final String apiVersion;

	public NotionApiClient(
		@Qualifier("notionWebClient") WebClient baseWebClient,
		NotionOAuthProperties properties
	) {
		this.baseWebClient = baseWebClient.mutate()
			.baseUrl(NOTION_API_BASE_URL)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
		this.apiVersion = properties.apiVersion();
	}

	/**
	 * 페이지 메타데이터 조회
	 */
	public Page getPage(String accessToken, String pageId) {
		log.debug("Notion 페이지 조회: {}", pageId);

		return buildClientWithAuth(accessToken)
			.get()
			.uri("/pages/{pageId}", pageId)
			.retrieve()
			.onStatus(HttpStatusCode::isError, response ->
				response.bodyToMono(String.class)
					.flatMap(body -> {
						log.error("Notion API 오류: {} - {}", response.statusCode(), body);
						return Mono.error(new NotionApiException(
							"페이지 조회 실패: " + response.statusCode() + " - " + body));
					}))
			.bodyToMono(Page.class)
			.block();
	}

	/**
	 * 블록 하위 목록 조회
	 */
	public BlockList getBlockChildren(String accessToken, String blockId) {
		return getBlockChildren(accessToken, blockId, null);
	}

	/**
	 * 블록 하위 목록 조회 (페이지네이션)
	 */
	public BlockList getBlockChildren(String accessToken, String blockId, String startCursor) {
		log.debug("Notion 블록 조회: {} (cursor: {})", blockId, startCursor);

		return buildClientWithAuth(accessToken)
			.get()
			.uri(uriBuilder -> {
				uriBuilder.path("/blocks/{blockId}/children")
					.queryParam("page_size", 100);
				if (startCursor != null) {
					uriBuilder.queryParam("start_cursor", startCursor);
				}
				return uriBuilder.build(blockId);
			})
			.retrieve()
			.onStatus(HttpStatusCode::isError, response ->
				response.bodyToMono(String.class)
					.flatMap(body -> {
						log.error("Notion API 오류: {} - {}", response.statusCode(), body);
						return Mono.error(new NotionApiException(
							"블록 조회 실패: " + response.statusCode() + " - " + body));
					}))
			.bodyToMono(BlockList.class)
			.block();
	}

	/**
	 * 특정 블록 조회
	 */
	public Block getBlock(String accessToken, String blockId) {
		log.debug("Notion 블록 단일 조회: {}", blockId);

		return buildClientWithAuth(accessToken)
			.get()
			.uri("/blocks/{blockId}", blockId)
			.retrieve()
			.onStatus(HttpStatusCode::isError, response ->
				response.bodyToMono(String.class)
					.flatMap(body -> {
						log.error("Notion API 오류: {} - {}", response.statusCode(), body);
						return Mono.error(new NotionApiException(
							"블록 조회 실패: " + response.statusCode() + " - " + body));
					}))
			.bodyToMono(Block.class)
			.block();
	}

	/**
	 * 사용자별 Access Token으로 인증된 WebClient 생성
	 */
	private WebClient buildClientWithAuth(String accessToken) {
		return baseWebClient.mutate()
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.defaultHeader("Notion-Version", apiVersion)
			.build();
	}
}
