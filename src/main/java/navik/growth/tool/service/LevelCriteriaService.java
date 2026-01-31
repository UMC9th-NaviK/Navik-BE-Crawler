package navik.growth.tool.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 직무 및 레벨별 평가 기준 관리 서비스
 * 역할: "이 직무의 이 레벨 유저는 어디에 중점을 두고 평가해야 하는가?"에 대한 AI 가이드라인 제공
 */
@Service
@RequiredArgsConstructor
public class LevelCriteriaService {

	/**
	 * 직무와 레벨에 맞는 점수 산정 가이드라인을 조회
	 * * @param jobId 직무 ID (1: Backend, 2: Frontend, 3: Design, 4: PM)
	 * @param levelValue 레벨 값 (1~10)
	 * @return 해당 직무 및 레벨의 평가 기준 텍스트
	 */
	public String findCriteria(Long jobId, Integer levelValue) {

		// 직무별 분기 처리
		return switch (jobId.intValue()) {
			case 1 -> getBackendCriteria(levelValue);
			case 2 -> getFrontendCriteria(levelValue);
			// case 3 -> getDesignCriteria(level);
			// case 4 -> getPmCriteria(level);
			default -> throw new IllegalStateException("Unexpected value: " + jobId.intValue());
		};
	}

	private String getBackendCriteria(int level) {
		if (level <= 3) {
			return """
				[Backend Level 1~3 (Beginner)]
				- **Focus**: 기본 문법, API 호출, CRUD 구현, 에러 해결 경험.
				- **Scoring**: 코드가 돌아가는지, 학습한 내용을 정리했는지가 중요합니다. 'Spring Boot 실행 성공', 'H2 DB 연동' 같은 기초적인 성공 경험에도 후하게(관대하게) 평가하세요.
				""";
		} else if (level <= 7) {
			return """
				[Backend Level 4~7 (Intermediate)]
				- **Focus**: 트랜잭션 관리, DB 설계(정규화/인덱스), 예외 처리 전략, 테스트 코드.
				- **Scoring**: 단순히 기능이 되는 것을 넘어 '안정성'을 고려했는지 봅니다. 트러블슈팅 과정(원인 분석->해결)이 없으면 감점하세요. 동시성 문제나 성능 최적화 시도가 보이면 가산점을 부여하세요.
				""";
		} else {
			return """
				[Backend Level 8~10 (Expert)]
				- **Focus**: MSA 아키텍처, 대용량 트래픽 처리, 고가용성(HA), 시스템 비용 최적화.
				- **Scoring**: 비즈니스 요구사항을 기술적으로 어떻게 풀어냈는지 '설계의 근거'를 평가하세요. 단순 구현은 0점입니다. 주니어 멘토링이나 기술 전파 내용이 포함되면 가산점입니다.
				""";
		}
	}

	private String getFrontendCriteria(int level) {
		if (level <= 3) {
			return """
				[Frontend Level 1~3 (Beginner)]
				- **Focus**: HTML/CSS 레이아웃, API 데이터 바인딩, React/Vue 기초 사용법.
				- **Scoring**: 화면이 의도대로 그려지는지, 컴포넌트를 만들어봤는지를 봅니다. UI 라이브러리를 사용해서라도 완성했다면 긍정적으로 평가하세요.
				""";
		} else if (level <= 7) {
			return """
				[Frontend Level 4~7 (Intermediate)]
				- **Focus**: 상태 관리(Redux/Recoil), 렌더링 최적화, 재사용 가능한 컴포넌트, 사용자 경험(UX).
				- **Scoring**: 불필요한 리렌더링을 막았는지, API 에러 상황을 UX적으로 잘 처리했는지 봅니다. SEO나 웹 접근성을 고려했다면 높은 점수를 주세요.
				""";
		} else {
			return """
				[Frontend Level 8~10 (Expert)]
				- **Focus**: 프론트엔드 아키텍처, 디자인 시스템 구축, 번들 사이즈 최적화, CI/CD 파이프라인.
				- **Scoring**: 대규모 프로젝트에서의 유지보수성을 어떻게 확보했는지 평가하세요. 성능 지표(Lighthouse 등) 개선 사례가 필수적입니다.
				""";
		}
	}
}