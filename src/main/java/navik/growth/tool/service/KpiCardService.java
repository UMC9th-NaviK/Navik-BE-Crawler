package navik.growth.tool.service;

import org.springframework.stereotype.Service;


/**
 * 직무별 KPI 카드 조회 서비스
 * TODO: 실제 DB 연동 구현 필요
 */
@Service
public class KpiCardService {

	/**
	 * 직무 ID로 해당 직무의 10개 KPI 카드 정보를 텍스트로 조회
	 *
	 * @param jobId 직무 ID (1:Backend, 2:Frontend, 3:Designer, 4:PM)
	 * @return KPI 카드 정보 JSON 텍스트 (10개 카드의 id, name, description 포함)
	 */
	public String findKpiCardsByJobId(Long jobId) {
		// 1. Product Manager (PM)
		if (jobId == 1L) {
			return """
        [
          {"id": 1, "name": "문제 정의·가설 수립", "description": "문제를 기능이나 요청 단위가 아니라 구조와 원인 중심으로 정의합니다. 데이터와 맥락을 바탕으로 검증 가능한 해결 가설을 세웁니다.", "category": "Strategy"},
          {"id": 2, "name": "데이터 기반 의사결정", "description": "의견이나 감이 아니라 정량 지표와 정성 리서치를 근거로 판단합니다. 지표를 통해 무엇을 먼저 해야 하는지를 명확히 합니다.", "category": "Strategy"},
          {"id": 3, "name": "서비스 구조·핵심 플로우 결정", "description": "서비스가 어떤 기능 구조를 가져야 하는지, 필수 사용자 흐름이 무엇인지 결정합니다. 이 결정은 이후 디자인·개발의 기준점이 됩니다.", "category": "Architecture"},
          {"id": 4, "name": "요구사항 정의·정책 설계", "description": "사용자와 비즈니스 요구를 정책과 규칙 형태로 정리합니다. 개발자가 바로 구현할 수 있도록 모호함 없이 문서화합니다.", "category": "Implementation"},
          {"id": 5, "name": "실험·검증 기반 의사결정", "description": "아이디어나 정책을 바로 밀지 않고 실험을 통해 검증합니다. 결과를 기준으로 유지·개선·중단을 판단합니다.", "category": "Strategy"},
          {"id": 6, "name": "우선순위·스코프 관리", "description": "모든 요청을 다 하지 않고 지금 가장 중요한 문제에 집중합니다. 리소스와 리스크를 고려해 범위를 조정하고 방향을 유지합니다.", "category": "Strategy"},
          {"id": 7, "name": "실행력·오너십", "description": "결정된 사항을 끝까지 밀고 가며 결과가 나올 때까지 책임집니다. 이슈 발생 시에도 조율과 복원을 통해 제품을 정상화합니다.", "category": "Collaboration"},
          {"id": 8, "name": "의사결정 정렬·협업 조율", "description": "각 직군의 관점을 하나의 의사결정 언어로 정리합니다. 갈등 상황에서도 방향이 흔들리지 않도록 팀을 정렬합니다.", "category": "Collaboration"},
          {"id": 9, "name": "AI/LLM 활용 기획", "description": "AI 기술의 가능성과 한계를 이해하고 제품 기능으로 연결합니다. 프롬프트와 흐름을 설계해 사용자 가치가 드러나도록 기획합니다.", "category": "Implementation"},
          {"id": 10, "name": "사용자 리서치·공감", "description": "사용자의 말과 행동을 통해 의사결정에 필요한 인사이트를 도출합니다. 공감에서 멈추지 않고 제품 개선으로 이어지게 합니다.", "category": "Strategy"}
        ]
        """;
		}

		// 2. Product Designer (UX/UI)
		if (jobId == 2L) {
			return """
        [
          {"id": 11, "name": "UX 전략·문제 재정의", "description": "주어진 요구를 그대로 받아들이지 않고, 사용자 관점에서 문제를 다시 정의합니다. 디자인이 해결해야 할 핵심 경험 과제를 명확히 합니다.", "category": "Strategy"},
          {"id": 12, "name": "정보 구조·사용자 플로우 설계", "description": "정보와 기능을 이해하기 쉬운 구조와 흐름으로 정리합니다. 사용자가 막히지 않고 목표에 도달하도록 시나리오를 설계합니다.", "category": "Design"},
          {"id": 13, "name": "UI 시각 디자인·비주얼 완성도", "description": "레이아웃, 타이포, 컬러를 통해 한눈에 이해되는 화면과 시각적 우선순위를 만듭니다.", "category": "Design"},
          {"id": 14, "name": "프로토타이핑·인터랙션 구현", "description": "실제 사용에 가까운 인터랙션을 구현해 디자인 의도가 행동으로 어떻게 전달되는지 검증합니다.", "category": "Implementation"},
          {"id": 15, "name": "디자인 시스템 구축·운영", "description": "컴포넌트와 가이드라인을 정리해 일관성과 확장성을 동시에 유지합니다. 팀 전체가 같은 기준으로 디자인할 수 있게 합니다.", "category": "Architecture"},
          {"id": 16, "name": "데이터 기반 UX 개선", "description": "사용 지표와 테스트 결과를 통해 UX 문제를 객관적으로 확인하고 개선 방향을 도출합니다. 감이 아닌 근거로 디자인을 수정합니다.", "category": "Strategy"},
          {"id": 17, "name": "AI 디자인 활용 능력", "description": "AI 도구를 활용해 디자인 탐색과 제작 과정을 효율화합니다. AI 결과를 바탕으로 디자이너의 판단을 더해 완성도를 높입니다.", "category": "Implementation"},
          {"id": 18, "name": "멀티 플랫폼(OS·Web·App) 이해", "description": "iOS, Android, Web 환경의 패턴과 제약을 이해하고 플랫폼에 맞는 경험을 설계합니다.", "category": "Design"},
          {"id": 19, "name": "협업·커뮤니케이션 역량", "description": "PM과 개발자가 이해할 수 있도록 디자인 의도와 결정을 명확한 언어로 전달합니다. 논의된 내용이 실제 구현에 반영되도록 정리합니다.", "category": "Collaboration"},
          {"id": 20, "name": "BX·BI 브랜드 경험 설계", "description": "브랜드의 톤과 메시지를 제품 전반의 사용자 경험에 자연스럽게 녹여냅니다. 화면 하나하나가 브랜드 인상을 만들도록 설계합니다.", "category": "Strategy"}
        ]
        """;
		}

		// 3. Frontend Developer
		if (jobId == 3L) {
			return """
        [
          {"id": 21, "name": "웹 기본기", "description": "DOM 구조, 스타일링, 비동기 처리 등 웹의 기본 동작 원리를 이해하고 안정적으로 구현합니다.(HTML·CSS·JavaScript·TypeScript 기반)", "category": "Foundation"},
          {"id": 22, "name": "프레임워크 숙련도", "description": "컴포넌트 구조와 상태 흐름을 이해하고, 프레임워크 환경에서 구조적으로 구현합니다.(React·Vue·TypeScript 등)", "category": "Implementation"},
          {"id": 23, "name": "상태관리·컴포넌트 아키텍처", "description": "재사용과 확장을 고려한 컴포넌트 구조를 설계합니다. 상태를 역할에 맞게 분리해 유지보수 가능한 구조를 만듭니다.", "category": "Architecture"},
          {"id": 24, "name": "웹 성능 최적화", "description": "성능 지표를 기준으로 병목 구간을 파악하고, 사용자가 체감하는 로딩과 인터랙션 경험을 개선합니다. (LCP, CLS 등 지표 기반)", "category": "Performance"},
          {"id": 25, "name": "API 연동·비동기 처리", "description": "API 호출 흐름을 안정적으로 구성하고, 로딩·에러·캐싱을 고려한 사용자 경험을 구현합니다.", "category": "Implementation"},
          {"id": 26, "name": "반응형·크로스 브라우징 대응", "description": "다양한 디바이스와 브라우저 환경에서도 일관된 화면과 동작을 유지합니다.", "category": "Quality"},
          {"id": 27, "name": "테스트 코드·품질 관리", "description": "테스트와 린트, 타입 체크를 통해 코드의 안정성과 신뢰도를 유지합니다.", "category": "Quality"},
          {"id": 28, "name": "Git·PR·협업 프로세스 이해", "description": "Git 흐름과 PR 문화를 이해하고, 리뷰와 협업을 통해 코드 품질을 함께 높입니다.", "category": "Collaboration"},
          {"id": 29, "name": "사용자 중심 UI 개발", "description": "디자인 의도와 맥락을 이해하고, 사용성과 인터랙션을 고려해 실제 사용 흐름에 맞게 UI를 구현합니다.", "category": "Implementation"},
          {"id": 30, "name": "빌드·도구 환경 이해", "description": "번들링과 개발 환경의 역할을 이해하고, 프로젝트의 규모와 특성에 맞게 적절한 도구 구성을 선택·관리합니다 (Vite/Webpack 등).", "category": "Infra"}
        ]
        """;
		}

		// 4. Backend Developer
		if (jobId == 4L) {
			return """
        [
          {"id": 31, "name": "주력 언어·프레임워크 숙련도", "description": "주력 언어와 프레임워크를 기반으로 비즈니스 요구사항을 안정적인 코드로 구현합니다. 실제 서비스 환경에서 예외 상황까지 고려해 기능을 완성합니다.", "category": "Implementation"},
          {"id": 32, "name": "REST API 설계·구현", "description": "리소스 중심으로 API 구조를 설계하고, 에러 규약과 응답 형식을 일관되게 정의합니다. 확장과 변경을 고려한 API를 구현합니다.", "category": "Design"},
          {"id": 33, "name": "DB·데이터 모델링", "description": "데이터 구조와 관계를 설계하고, 쿼리와 인덱스를 고려해 성능을 최적화합니다. 대용량 데이터 상황에서도 안정적인 접근을 유지합니다.", "category": "Design"},
          {"id": 34, "name": "아키텍처 설계", "description": "서비스 확장과 변경을 고려해 모듈 간 책임이 분리된 구조(MSA/DDD 등)를 설계합니다. 운영과 유지보수가 가능한 아키텍처를 구성합니다.", "category": "Architecture"},
          {"id": 35, "name": "클라우드·DevOps 환경 이해", "description": "배포와 인프라 흐름을 이해하고, 개발부터 운영까지 이어지는 CI/CD 및 보안 환경을 구성합니다. 서비스가 안정적으로 배포·운영되도록 관리합니다.", "category": "Infra"},
          {"id": 36, "name": "성능·트래픽 처리 최적화", "description": "트래픽 증가 상황을 가정해 병목 지점을 분석하고, 캐시와 비동기 처리 전략을 적용합니다. 부하 상황에서도 서비스 안정성을 유지합니다.", "category": "Performance"},
          {"id": 37, "name": "보안·인증·권한 처리", "description": "인증과 권한 흐름(OAuth/JWT 등)을 설계해 사용자와 시스템 접근을 안전하게 제어합니다. 보안 이슈를 예방할 수 있는 구조를 유지합니다.", "category": "Security"},
          {"id": 38, "name": "테스트·코드 품질 관리", "description": "테스트 코드와 리팩토링을 통해 코드의 안정성과 가독성을 유지합니다. 변경이 반복되어도 품질이 무너지지 않게 관리합니다.", "category": "Quality"},
          {"id": 39, "name": "협업·문서화·의사결정 기록", "description": "API 명세와 기술 문서를 정리해 팀 간 협업이 원활히 이루어지도록 합니다. 논의와 기술적 의사결정(ADR)이 이후 작업에 이어지도록 기록합니다.", "category": "Collaboration"},
          {"id": 40, "name": "운영·모니터링·장애 대응", "description": "로그와 모니터링을 통해 이상 징후를 빠르게 감지합니다. 장애 발생 시 원인을 파악하고 서비스를 안정적으로 복원합니다.", "category": "Operation"}
        ]
        """;
		}

		return "[]";
	}
}
