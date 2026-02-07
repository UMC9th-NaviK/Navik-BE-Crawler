package navik.growth.analysis.service.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ContentTypeHelper {
    public enum ContentType {
        NOTION_LINK,
        GITHUB_PR_LINK,
        TEXT
    }
    /**
	 * 컨텐츠 타입 판별
	 */
	public ContentType detectContentType(String content) {
		String trimmed = content.trim();
		if (trimmed.matches("^https?://.*notion\\.(so|site)/.*$")) {
			return ContentType.NOTION_LINK;
		}
		if (trimmed.matches("^https?://github\\.com/.+/pull/\\d+.*$")) {
			return ContentType.GITHUB_PR_LINK;
		}
		return ContentType.TEXT;
	}

	/**
	 * 컨텐츠 타입에 따라 필요한 Tool 목록 구성
	 * - 공통: retrieveLevelCriteria
	 * - Notion 링크: + fetchNotionPage
	 * - GitHub PR 링크: + fetchGitHubPR
	 */
	public List<String> buildToolNames(ContentType contentType) {
		List<String> tools = new ArrayList<>(List.of("retrieveLevelCriteria"));
		switch (contentType) {
			case NOTION_LINK -> tools.add("fetchNotionPage");
			case GITHUB_PR_LINK -> tools.add("fetchGitHubPR");
			case TEXT -> { /* 텍스트는 추가 Tool 불필요 */ }
		}
		return tools;
	}
}
