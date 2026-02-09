package navik.growth.tool.service;

import org.springframework.stereotype.Service;

/**
 * 직무별 핵심 책임 및 명시적 제외 항목 조회 서비스
 * TODO: 실제 DB 연동 구현 필요
 */
@Service
public class JobScopeService {

	/**
	 * 직무 ID로 해당 직무의 핵심 책임(coreResponsibilities)과 명시적 제외 항목(explicitlyExcluded)을 조회
	 *
	 * @param jobId 직무 ID (1:Backend, 2:Frontend, 3:Designer, 4:PM)
	 * @return 직무 범위 정보 JSON 텍스트
	 */
	public String findJobScopeByJobId(Long jobId) {
		// 1. PM
		if (jobId == 1L) {
			return """
                {
                  "jobId": 1,
                  "jobTitle": "Product Manager",
                  "coreResponsibilities": [
                    "문제 정의·가설 수립",
                    "데이터 기반 의사결정",
                    "요구사항 정의",
                    "우선순위·스코프 관리",
                    "실험·검증 설계",
                    "협업 조율"
                  ],
                  "explicitlyExcluded": [
                    "직접 코드 구현",
                    "UI 시각 디자인",
                    "DB 스키마 설계",
                    "인프라 운영"
                  ]
                }
                """;
		}

		if (jobId == 2L) {
			return """
                {
                  "jobId": 2,
                  "jobTitle": "Product Designer",
                  "coreResponsibilities": [
                    "UX 전략·문제 정의",
                    "정보 구조·사용자 플로우 설계",
                    "UI 시각 디자인",
                    "프로토타이핑",
                    "디자인 시스템 구축",
                    "사용성 테스트"
                  ],
                  "explicitlyExcluded": [
                    "서버 코드 구현",
                    "DB 설계",
                    "API 개발",
                    "인프라 운영"
                  ]
                }
                """;
		}

		// 3. 프론트
		if (jobId == 3L) {
			return """
                {
                  "jobId": 3,
                  "jobTitle": "Frontend Developer",
                  "coreResponsibilities": [
                    "웹 UI 구현 및 인터랙션 개발",
                    "프레임워크 기반 컴포넌트 설계",
                    "웹 성능 최적화",
                    "API 연동·비동기 처리",
                    "반응형·크로스 브라우저 대응",
                    "상태관리 설계"
                  ],
                  "explicitlyExcluded": [
                    "서버 인프라 운영",
                    "DB 스키마 설계",
                    "그래픽 디자인 원화 제작",
                    "사용자 리서치"
                  ]
                }
                """;
		}

		// 4. 백엔드
		if (jobId == 4L) {
			return """
                {
                  "jobId": 4,
                  "jobTitle": "Backend Developer",
                  "coreResponsibilities": [
                    "서버 사이드 비즈니스 로직 구현",
                    "API 설계 및 성능 개선",
                    "DB·데이터 모델링",
                    "서버 아키텍처 설계",
                    "클라우드·인프라 운영",
                    "보안·인증 처리",
                    "테스트·코드 품질 관리"
                  ],
                  "explicitlyExcluded": [
                    "UI 디자인",
                    "그래픽 작업",
                    "사용자 리서치",
                    "브랜드 디자인"
                  ]
                }
                """;
		}

		// 해당하는 직무가 없을 경우 빈 객체 반환
		return "{}";
	}
}
