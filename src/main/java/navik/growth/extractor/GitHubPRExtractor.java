package navik.growth.extractor;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import navik.growth.extractor.dto.GitHubPRRequestResponse.GitHubPR;
import navik.growth.extractor.dto.GitHubPRRequestResponse.GitHubPRFile;

/**
 * GitHub Public PR 정보를 추출하는 컴포넌트
 */
@Slf4j
@Component
public class GitHubPRExtractor {

	private final WebClient webClient = WebClient.builder()
		.baseUrl("https://api.github.com")
		.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
		.defaultHeader(HttpHeaders.USER_AGENT, "NaviK-Growth-Analyzer")
		.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
		.build();

	/**
	 * GitHub Public PR 정보 추출
	 * 예시 URL: https://github.com/owner/repo/pull/123
	 *
	 * @param url GitHub Pull Request URL
	 * @return 마크다운 형식의 PR 정보
	 */
	public String extractPublicPR(String url) {
		try {
			log.info("GitHub PR 추출 시작: {}", url);

			// URL 파싱: https://github.com/{owner}/{repo}/pull/{number}
			PRInfo prInfo = parsePRUrl(url);

			// 1. PR 기본 정보 조회
			GitHubPR pr = webClient.get()
				.uri("/repos/{owner}/{repo}/pulls/{number}", prInfo.owner(), prInfo.repo(), prInfo.number())
				.retrieve()
				.bodyToMono(GitHubPR.class)
				.block();

			if (pr == null) {
				throw new IllegalStateException("GitHub PR을 가져오는데 실패했습니다.");
			}

			// 2. PR Files 조회 (변경된 파일 목록)
			GitHubPRFile[] files = webClient.get()
				.uri("/repos/{owner}/{repo}/pulls/{number}/files", prInfo.owner(), prInfo.repo(), prInfo.number())
				.retrieve()
				.bodyToMono(GitHubPRFile[].class)
				.block();

			// 3. 마크다운 형식으로 변환
			String markdown = buildMarkdown(pr, files);

			log.info("GitHub PR 추출 완료: {} (길이: {}자)", url, markdown.length());

			return markdown;

		} catch (Exception e) {
			log.error("GitHub PR 추출 실패: {}", url, e);
			throw new RuntimeException("GitHub PR 추출 실패: " + url, e);
		}
	}

	private PRInfo parsePRUrl(String url) {
		// https://github.com/{owner}/{repo}/pull/{number} 형식 파싱
		String cleanUrl = url.replace("https://github.com/", "")
			.replace("http://github.com/", "");

		String[] parts = cleanUrl.split("/");

		if (parts.length < 4 || !"pull".equals(parts[2])) {
			throw new IllegalArgumentException("유효하지 않은 GitHub PR URL입니다: " + url);
		}

		return new PRInfo(parts[0], parts[1], parts[3]);
	}

	private String buildMarkdown(GitHubPR pr, GitHubPRFile[] files) {
		StringBuilder markdown = new StringBuilder();

		// PR 제목
		markdown.append("# PR: ").append(pr.title()).append("\n\n");

		// 기본 정보
		markdown.append("**Author:** ").append(pr.user() != null ? pr.user().login() : "Unknown").append("\n");
		markdown.append("**State:** ").append(pr.state()).append("\n");
		markdown.append("**Created:** ").append(pr.createdAt()).append("\n");
		markdown.append("**Base Branch:** ").append(pr.base() != null ? pr.base().ref() : "Unknown").append("\n");
		markdown.append("**Head Branch:** ").append(pr.head() != null ? pr.head().ref() : "Unknown").append("\n\n");

		// PR 설명
		if (pr.body() != null && !pr.body().isEmpty()) {
			markdown.append("## Description\n\n");
			markdown.append(pr.body()).append("\n\n");
		}

		// 변경된 파일 목록
		if (files != null && files.length > 0) {
			markdown.append("## Changed Files (").append(files.length).append(" files)\n\n");

			for (GitHubPRFile file : files) {
				markdown.append("### ").append(file.filename()).append("\n");
				markdown.append("- **Status:** ").append(file.status()).append("\n");
				markdown.append("- **Additions:** +").append(file.additions()).append("\n");
				markdown.append("- **Deletions:** -").append(file.deletions()).append("\n");

				// Patch 내용 (diff) - 길이 제한
				if (file.patch() != null && !file.patch().isEmpty()) {
					String patch = file.patch();
					if (patch.length() > 2000) {
						patch = patch.substring(0, 2000) + "\n... (truncated)";
					}
					markdown.append("\n```diff\n");
					markdown.append(patch);
					markdown.append("\n```\n");
				}
				markdown.append("\n");
			}
		}

		// 총 변경 통계
		markdown.append("## Summary\n\n");
		markdown.append("**Total Changes:** +").append(pr.additions())
			.append(" / -").append(pr.deletions())
			.append(" (").append(pr.changedFiles()).append(" files)\n");
		markdown.append("**Commits:** ").append(pr.commits()).append("\n");

		return markdown.toString();
	}

	// Inner Records
	private record PRInfo(String owner, String repo, String number) {
	}
}