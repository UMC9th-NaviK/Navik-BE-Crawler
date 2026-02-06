package navik.growth.analysis.strategy;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import navik.ai.util.PromptLoader;

/**
 * 직무(JobId)에 맞는 평가 페르소나(시스템 프롬프트)를 로드하는 전략 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class PersonaPromptLoader {

	private final PromptLoader promptLoader;

	private static final String BASE_INSTRUCTION_PATH = "classpath:prompts/growth/base-instruction.txt";
	private static final String BACKEND_ENGINEER_PATH = "classpath:prompts/growth/personas/backend-engineer.txt";
	private static final String FRONTEND_ENGINEER_PATH = "classpath:prompts/growth/personas/frontend-engineer.txt";
	private static final String PRODUCT_DESIGNER_PATH = "classpath:prompts/growth/personas/product-designer.txt";
	private static final String PRODUCT_MANAGER_PATH = "classpath:prompts/growth/personas/product-manager.txt";
	private static final String DEFAULT_COACH_PATH = "classpath:prompts/growth/personas/default-coach.txt";

	/**
	 * JobId에 해당하는 평가 페르소나 시스템 프롬프트를 반환
	 *
	 * @param jobId 직무 ID
	 * @return 시스템 프롬프트 텍스트
	 */
	public String load(Long jobId) {
		String baseInstruction = promptLoader.loadPrompt(BASE_INSTRUCTION_PATH);
		String personaDefinition = getPersonaDefinition(jobId);
		return baseInstruction + personaDefinition;
	}

	// 직무별 페르소나 정의 (Strategy Pattern의 텍스트 버전)
	private String getPersonaDefinition(Long jobId) {
		String personaPath = switch (jobId.intValue()) {
			case 1 -> BACKEND_ENGINEER_PATH;
			case 2 -> FRONTEND_ENGINEER_PATH;
			case 3 -> PRODUCT_DESIGNER_PATH;
			case 4 -> PRODUCT_MANAGER_PATH;
			default -> DEFAULT_COACH_PATH;
		};

		return promptLoader.loadPrompt(personaPath);
	}
}
