package navik.growth.tool.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import navik.ai.util.PromptLoader;

/**
 * 직무 및 레벨별 평가 기준 관리 서비스
 * 역할: "이 직무의 이 레벨 유저는 어디에 중점을 두고 평가해야 하는가?"에 대한 AI 가이드라인 제공
 */
@Service
@RequiredArgsConstructor
public class LevelCriteriaService {

	private static final String CRITERIA_PATH_TEMPLATE = "classpath:prompts/growth/criteria/%s/level-%d.txt";

	private static final Map<Integer, String> JOB_NAME_MAP = Map.of(
		1, "pm",
		2, "designer",
		3, "frontend",
		4, "backend"
	);

	private final PromptLoader promptLoader;

	/**
	 * 직무와 레벨에 맞는 점수 산정 가이드라인을 조회
	 *
	 * @param jobId 직무 ID
	 * @param levelValue 레벨 값 (1~10)
	 * @return 해당 직무 및 레벨의 평가 기준 텍스트
	 */
	public String findCriteria(Long jobId, Integer levelValue) {
		String jobName = JOB_NAME_MAP.get(jobId.intValue());
		if (jobName == null) {
			throw new IllegalStateException("Unexpected jobId: " + jobId);
		}

		String path = String.format(CRITERIA_PATH_TEMPLATE, jobName, levelValue);
		return promptLoader.loadPrompt(path);
	}
}
