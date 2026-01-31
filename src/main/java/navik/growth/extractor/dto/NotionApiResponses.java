package navik.growth.extractor.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotionApiResponses {

	/**
	 * 페이지 조회 응답
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Page(
		String id,
		String object,
		Map<String, Property> properties
	) {
	}

	/**
	 * 블록 목록 조회 응답
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record BlockList(
		String object,
		List<Block> results,
		@JsonProperty("has_more") boolean hasMore,
		@JsonProperty("next_cursor") String nextCursor
	) {
	}

	/**
	 * 블록
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Block(
		String id,
		String type,
		@JsonProperty("has_children") boolean hasChildren,
		@JsonProperty("paragraph") TextBlock paragraph,
		@JsonProperty("heading_1") TextBlock heading1,
		@JsonProperty("heading_2") TextBlock heading2,
		@JsonProperty("heading_3") TextBlock heading3,
		@JsonProperty("bulleted_list_item") TextBlock bulletedListItem,
		@JsonProperty("numbered_list_item") TextBlock numberedListItem,
		@JsonProperty("to_do") ToDoBlock toDo,
		@JsonProperty("toggle") TextBlock toggle,
		@JsonProperty("quote") TextBlock quote,
		@JsonProperty("callout") CalloutBlock callout,
		@JsonProperty("code") CodeBlock code,
		@JsonProperty("divider") Object divider,
		@JsonProperty("image") ImageBlock image,
		@JsonProperty("child_page") ChildPage childPage
	) {
	}

	/**
	 * 텍스트 블록 (paragraph, heading, list 등)
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TextBlock(
		@JsonProperty("rich_text") List<RichText> richText,
		String color
	) {
	}

	/**
	 * 체크박스 블록
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ToDoBlock(
		@JsonProperty("rich_text") List<RichText> richText,
		boolean checked
	) {
	}

	/**
	 * 콜아웃 블록
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CalloutBlock(
		@JsonProperty("rich_text") List<RichText> richText,
		Icon icon
	) {
	}

	/**
	 * 코드 블록
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CodeBlock(
		@JsonProperty("rich_text") List<RichText> richText,
		String language
	) {
	}

	/**
	 * 이미지 블록
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ImageBlock(
		String type,
		FileObject file,
		ExternalObject external
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FileObject(String url) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ExternalObject(String url) {
	}

	/**
	 * 자식 페이지 참조
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChildPage(String title) {
	}

	/**
	 * 아이콘
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Icon(
		String type,
		String emoji
	) {
	}

	/**
	 * Rich Text
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record RichText(
		String type,
		Text text,
		Annotations annotations,
		@JsonProperty("plain_text") String plainText,
		String href
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Text(
		String content,
		Link link
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Link(String url) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Annotations(
		boolean bold,
		boolean italic,
		boolean strikethrough,
		boolean underline,
		boolean code,
		String color
	) {
	}

	/**
	 * 프로퍼티
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Property(
		String id,
		String type,
		List<RichText> title
	) {
	}
}
