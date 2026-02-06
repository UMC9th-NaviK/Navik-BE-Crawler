package navik.ai.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Spring Resource 추상화를 활용한 프롬프트 로더
 * 외부 파일에서 프롬프트를 읽어오는 유틸리티 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class PromptLoader {

	private final ResourceLoader resourceLoader;

	/**
	 * 지정된 경로의 프롬프트 파일을 읽어옵니다.
	 *
	 * @param path classpath 기준 경로 (예: "classpath:prompts/recruitment/system-prompt.txt")
	 * @return 파일 내용
	 * @throws RuntimeException 파일을 읽을 수 없는 경우
	 */
	public String loadPrompt(String path) {
		try {
			Resource resource = resourceLoader.getResource(path);
			if (!resource.exists()) {
				throw new IllegalArgumentException("Prompt file not found: " + path);
			}
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load prompt from: " + path, e);
		}
	}

	/**
	 * 여러 프롬프트 파일을 읽어서 하나로 합칩니다.
	 *
	 * @param paths classpath 기준 경로들
	 * @return 합쳐진 프롬프트 내용
	 */
	public String loadAndMergePrompts(String... paths) {
		StringBuilder merged = new StringBuilder();
		for (String path : paths) {
			merged.append(loadPrompt(path));
		}
		return merged.toString();
	}
}
