package navik.growth.extractor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.growth.extractor.dto.NotionApiResponses.Block;
import navik.growth.extractor.dto.NotionApiResponses.BlockList;
import navik.growth.extractor.dto.NotionApiResponses.Page;
import navik.growth.extractor.dto.NotionApiResponses.RichText;
import navik.growth.notion.api.NotionApiClient;
import navik.growth.notion.dto.NotionWorkspaceToken;
import navik.growth.notion.exception.NotionApiException;
import navik.growth.notion.repository.NotionTokenRepository;

/**
 * Notion API를 통해 페이지 컨텐츠를 추출하는 컴포넌트
 * 사용자별 OAuth 토큰으로 접근
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotionPageExtractor {

	private static final Pattern PAGE_ID_PATTERN = Pattern.compile(
		"([a-f0-9]{32}|[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})$"
	);

	private final NotionApiClient notionApiClient;
	private final NotionTokenRepository tokenRepository;

	/**
	 * 노션 페이지에서 컨텐츠 추출
	 *
	 * @param userId 사용자 ID (OAuth 토큰 조회용)
	 * @param url    노션 페이지 URL
	 * @return 마크다운 형식의 컨텐츠
	 */
	public String extractPage(String userId, String url) {
		log.info("노션 페이지 추출 시작: userId={}, url={}", userId, url);

		// 1. 사용자의 모든 워크스페이스 토큰 조회
		List<NotionWorkspaceToken> tokens = tokenRepository.findAllByUserId(userId);
		if (tokens.isEmpty()) {
			throw new NotionApiException("Notion이 연동되지 않았습니다. userId=" + userId);
		}

		// 2. URL에서 페이지 ID 추출
		String pageId = extractPageId(url);
		log.debug("추출된 페이지 ID: {}", pageId);

		// 3. 토큰이 1개면 바로 사용 (fast path)
		if (tokens.size() == 1) {
			return extractPageWithToken(tokens.get(0).accessToken(), pageId, url);
		}

		// 4. 여러 토큰이면 각각 시도하여 성공하는 토큰 사용
		for (NotionWorkspaceToken token : tokens) {
			try {
				Page page = notionApiClient.getPage(token.accessToken(), pageId);
				log.info("워크스페이스 매칭 성공: workspaceId={}", token.workspaceId());
				return extractPageContent(token.accessToken(), pageId, page, url);
			} catch (Exception e) {
				log.debug("워크스페이스 접근 실패 (다음 시도): workspaceId={}, error={}",
					token.workspaceId(), e.getMessage());
			}
		}

		// 5. 모든 토큰 실패
		throw new NotionApiException(
			"연결된 워크스페이스 중 해당 페이지에 접근 가능한 것이 없습니다: " + url);
	}

	private String extractPageWithToken(String accessToken, String pageId, String url) {
		try {
			Page page = notionApiClient.getPage(accessToken, pageId);
			return extractPageContent(accessToken, pageId, page, url);
		} catch (Exception e) {
			log.error("노션 페이지 추출 실패: url={}", url, e);
			throw new RuntimeException("노션 페이지 추출 실패: " + url, e);
		}
	}

	private String extractPageContent(String accessToken, String pageId, Page page, String url) {
		String title = extractTitle(page);

		StringBuilder content = new StringBuilder();
		content.append("# ").append(title).append("\n\n");

		extractBlocks(accessToken, pageId, content, 0);

		log.info("노션 페이지 추출 완료: {} (길이: {}자)", url, content.length());
		return content.toString();
	}

	/**
	 * URL에서 페이지 ID 추출
	 */
	private String extractPageId(String url) {
		// URL 정규화: 쿼리 파라미터 제거
		String cleanUrl = url.split("\\?")[0];

		// 페이지 ID 패턴 매칭 (32자 hex 또는 UUID 형식)
		Matcher matcher = PAGE_ID_PATTERN.matcher(cleanUrl);
		if (matcher.find()) {
			String pageId = matcher.group(1);
			// 하이픈 없는 형식을 UUID 형식으로 변환
			if (!pageId.contains("-") && pageId.length() == 32) {
				return pageId.substring(0, 8) + "-" +
					pageId.substring(8, 12) + "-" +
					pageId.substring(12, 16) + "-" +
					pageId.substring(16, 20) + "-" +
					pageId.substring(20);
			}
			return pageId;
		}

		throw new IllegalArgumentException("유효한 노션 페이지 URL이 아닙니다: " + url);
	}

	/**
	 * 페이지 제목 추출
	 */
	private String extractTitle(Page page) {
		if (page.properties() != null) {
			var titleProp = page.properties().get("title");
			if (titleProp != null && titleProp.title() != null && !titleProp.title().isEmpty()) {
				return titleProp.title().stream()
					.map(RichText::plainText)
					.reduce("", String::concat);
			}

			// Name 프로퍼티도 확인 (데이터베이스 페이지의 경우)
			var nameProp = page.properties().get("Name");
			if (nameProp != null && nameProp.title() != null && !nameProp.title().isEmpty()) {
				return nameProp.title().stream()
					.map(RichText::plainText)
					.reduce("", String::concat);
			}
		}
		return "제목 없음";
	}

	/**
	 * 블록 재귀적 추출
	 */
	private void extractBlocks(String accessToken, String blockId, StringBuilder content, int depth) {
		String cursor = null;

		do {
			BlockList blockList = notionApiClient.getBlockChildren(accessToken, blockId, cursor);

			for (Block block : blockList.results()) {
				convertBlockToMarkdown(block, content, depth);

				// 자식 블록이 있으면 재귀 호출
				if (block.hasChildren()) {
					extractBlocks(accessToken, block.id(), content, depth + 1);
				}
			}

			cursor = blockList.hasMore() ? blockList.nextCursor() : null;
		} while (cursor != null);
	}

	/**
	 * 블록을 마크다운으로 변환
	 */
	private void convertBlockToMarkdown(Block block, StringBuilder content, int depth) {
		String indent = "  ".repeat(depth);
		String type = block.type();

		switch (type) {
			case "paragraph" -> {
				String text = extractRichText(block.paragraph().richText());
				if (!text.isEmpty()) {
					content.append(indent).append(text).append("\n\n");
				}
			}
			case "heading_1" -> {
				String text = extractRichText(block.heading1().richText());
				content.append("# ").append(text).append("\n\n");
			}
			case "heading_2" -> {
				String text = extractRichText(block.heading2().richText());
				content.append("## ").append(text).append("\n\n");
			}
			case "heading_3" -> {
				String text = extractRichText(block.heading3().richText());
				content.append("### ").append(text).append("\n\n");
			}
			case "bulleted_list_item" -> {
				String text = extractRichText(block.bulletedListItem().richText());
				content.append(indent).append("- ").append(text).append("\n");
			}
			case "numbered_list_item" -> {
				String text = extractRichText(block.numberedListItem().richText());
				content.append(indent).append("1. ").append(text).append("\n");
			}
			case "to_do" -> {
				String text = extractRichText(block.toDo().richText());
				String checkbox = block.toDo().checked() ? "[x]" : "[ ]";
				content.append(indent).append("- ").append(checkbox).append(" ").append(text).append("\n");
			}
			case "toggle" -> {
				String text = extractRichText(block.toggle().richText());
				content.append(indent).append("<details>\n");
				content.append(indent).append("<summary>").append(text).append("</summary>\n\n");
				// 자식은 extractBlocks에서 처리됨
			}
			case "quote" -> {
				String text = extractRichText(block.quote().richText());
				content.append(indent).append("> ").append(text).append("\n\n");
			}
			case "callout" -> {
				String text = extractRichText(block.callout().richText());
				String emoji = block.callout().icon() != null && block.callout().icon().emoji() != null
					? block.callout().icon().emoji() + " "
					: "";
				content.append(indent).append("> ").append(emoji).append(text).append("\n\n");
			}
			case "code" -> {
				String text = extractRichText(block.code().richText());
				String language = block.code().language() != null ? block.code().language() : "";
				content.append(indent).append("```").append(language).append("\n");
				content.append(text).append("\n");
				content.append(indent).append("```\n\n");
			}
			case "divider" -> content.append("\n---\n\n");
			case "image" -> {
				String imageUrl = "";
				if (block.image() != null) {
					if ("file".equals(block.image().type()) && block.image().file() != null) {
						imageUrl = block.image().file().url();
					} else if ("external".equals(block.image().type()) && block.image().external() != null) {
						imageUrl = block.image().external().url();
					}
				}
				if (!imageUrl.isEmpty()) {
					content.append(indent).append("![").append("image").append("](").append(imageUrl).append(")\n\n");
				}
			}
			case "child_page" -> {
				if (block.childPage() != null) {
					content.append(indent).append("**[").append(block.childPage().title()).append("]**\n\n");
				}
			}
			default -> {
				// 지원하지 않는 블록 타입은 무시
				log.debug("지원하지 않는 블록 타입: {}", type);
			}
		}
	}

	/**
	 * RichText 배열에서 텍스트 추출 (서식 적용)
	 */
	private String extractRichText(List<RichText> richTexts) {
		if (richTexts == null || richTexts.isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (RichText rt : richTexts) {
			String text = rt.plainText();
			if (text == null)
				continue;

			// 서식 적용
			if (rt.annotations() != null) {
				if (rt.annotations().code()) {
					text = "`" + text + "`";
				}
				if (rt.annotations().bold()) {
					text = "**" + text + "**";
				}
				if (rt.annotations().italic()) {
					text = "*" + text + "*";
				}
				if (rt.annotations().strikethrough()) {
					text = "~~" + text + "~~";
				}
			}

			// 링크 적용
			if (rt.href() != null) {
				text = "[" + text + "](" + rt.href() + ")";
			}

			result.append(text);
		}
		return result.toString();
	}
}