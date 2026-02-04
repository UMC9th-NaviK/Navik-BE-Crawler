package navik.growth.tool.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 직무 및 레벨별 평가 기준 관리 서비스
 * 역할: "이 직무의 이 레벨 유저는 어디에 중점을 두고 평가해야 하는가?"에 대한 AI 가이드라인 제공
 * 이제 KPI 카테고리별로 세분화된 기준을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class LevelCriteriaService {

	/**
	 * 직무와 레벨에 맞는 점수 산정 가이드라인을 조회
	 * @param jobId 직무 ID (1: Backend, 2: Frontend, 3: Design, 4: PM)
	 * @param levelValue 레벨 값 (1~10)
	 * @return 해당 직무, 레벨, KPI 카테고리별 평가 기준 텍스트
	 */
	public String findCriteria(Long jobId, Integer levelValue) {
		// 직무별 분기 처리
		return switch (jobId.intValue()) {
			case 1 -> getBackendCriteria(levelValue);
			case 2 -> getFrontendCriteria(levelValue);
			case 3 -> getDesignCriteria(levelValue);
			case 4 -> getPmCriteria(levelValue);
			default -> throw new IllegalStateException("Unexpected value: " + jobId.intValue());
		};
	}

	private String getBackendCriteria(int level) {
		if (level <= 3) {
			return """
				[Backend Level 1~3 (Beginner) Scoring Guide]
				- **General**: 학습한 내용을 코드로 '성공'시킨 경험에 관대하게 점수를 부여하십시오.
				- **Implementation**: CRUD 구현, API 호출 등 기본적인 기능 완수를 높게 평가합니다.
				- **Design**: 정해진 규격에 따라 API를 만들거나, 간단한 테이블 설계를 시도한 경우 점수를 부여합니다.
				- **Architecture**: 설계 내용을 이해하고 활용하려는 시도에 점수를 줍니다.
				- **Quality**: 코드 가독성을 높이려는 노력(주석, 변수명 등)이 보이면 점수를 줍니다.
				- **Performance, Security, Infra, Operation**: 이 레벨에서는 깊게 평가하지 않습니다. 관련 용어를 학습하고 정리한 수준이라면 긍정적으로 평가합니다.
				""";
		} else if (level <= 7) {
			return """
				[Backend Level 4~7 (Intermediate) Scoring Guide]
				- **General**: 기능 구현을 넘어 '왜' 그렇게 만들었는지, '안정성'을 고려했는지 중점적으로 평가하십시오.
				- **Implementation**: 비즈니스 로직의 복잡성을 해결하거나, 대용량 데이터를 효과적으로 처리한 경험을 높게 평가합니다.
				- **Design**: 트랜잭션 관리, 데이터 정규화, 인덱스 설계 등 안정성을 고려한 설계에 높은 점수를 부여합니다.
				- **Architecture**: MSA, DDD 등 특정 아키텍처를 적용하고 그 장단점을 이해하고 있다면 가산점을 부여합니다.
				- **Quality**: 테스트 코드 작성(Unit, Integration) 및 리팩토링 경험은 필수적으로 평가합니다.
				- **Performance**: 병목 지점을 분석하고 캐시, 비동기 처리 등으로 성능을 개선한 구체적인 경험을 높게 평가합니다.
				- **Security**: OAuth/JWT 적용, SQL Injection 방어 등 보안 원칙을 이해하고 적용한 경우 가산점을 부여합니다.
				- **Infra, Operation**: CI/CD 환경을 직접 개선하거나, 로깅/모니터링 시스템을 구축하여 문제를 해결한 경험을 높게 평가합니다.
				""";
		} else {
			return """
				[Backend Level 8~10 (Expert) Scoring Guide]
				- **General**: 기술을 통해 비즈니스 문제를 어떻게 해결했는지, '설계의 근거'와 '트레이드오프'를 중심으로 평가하십시오. 단순 기능 구현은 0점입니다.
				- **Implementation**: 복잡한 비즈니스 요구사항을 확장 가능하고 유지보수 가능한 코드로 구현한 능력을 평가합니다.
				- **Design**: 대규모 서비스를 위한 데이터 모델링, 분산 시스템에서의 일관성 처리 등 고난이도 설계 경험을 높게 평가합니다.
				- **Architecture**: 기술 트렌드를 비판적으로 수용하여 팀의 기술 스택과 아키텍처 방향성을 제시하고, 그 결과를 비즈니스 임팩트로 증명한 경우 최고점을 부여합니다.
				- **Quality**: 코드 리뷰, 기술 표준 수립, 멘토링 등을 통해 팀 전체의 코드 품질을 향상시킨 경험을 높게 평가합니다.
				- **Performance, Security**: 시스템 전반의 성능 및 보안 취약점을 분석하고, 비용 효율적인 해결책을 제시하고 실행한 경험을 높게 평가합니다.
				- **Infra, Operation**: 고가용성(HA)을 보장하는 인프라를 설계하거나, 장애 대응 프로세스를 구축하여 서비스 안정성을 크게 향상시킨 경험을 높게 평가합니다.
				""";
		}
	}

	private String getFrontendCriteria(int level) {
		if (level <= 3) {
			return """
				[Frontend Level 1~3 (Beginner) Scoring Guide]
				- **General**: 학습한 내용을 바탕으로 화면을 '완성'시킨 경험에 관대하게 점수를 부여하십시오.
				- **Foundation**: HTML 시맨틱, CSS 레이아웃, JS 기본 문법 등 기초 지식 학습 및 적용 경험을 높게 평가합니다.
				- **Implementation**: API 데이터를 화면에 바인딩하거나, 기본적인 UI 인터랙션을 구현한 경험에 점수를 부여합니다.
				- **Architecture**: 컴포넌트 재사용을 시도하거나, 상태 관리의 필요성을 이해하려는 노력에 점수를 줍니다.
				- **Quality**: UI 라이브러리를 활용해서라도 일관된 디자인을 적용하려는 노력을 긍정적으로 평가합니다.
				- **Performance, Infra, Collaboration**: 이 레벨에서는 깊게 평가하지 않습니다. 관련 용어를 학습하고 정리한 수준이라면 긍정적으로 평가합니다.
				""";
		} else if (level <= 7) {
			return """
				[Frontend Level 4~7 (Intermediate) Scoring Guide]
				- **General**: 화면 구현을 넘어 '사용자 경험(UX)'과 '성능'을 개선하기 위한 고민과 시도를 중점적으로 평가하십시오.
				- **Foundation**: 브라우저 렌더링 원리를 이해하고 이를 바탕으로 성능을 개선한 경험을 높게 평가합니다.
				- **Implementation**: 복잡한 상태 관리(Recoil, Redux 등) 로직을 설계하거나, 커스텀 훅/고차 컴포넌트(HOC)를 구현한 경험을 높게 평가합니다.
				- **Architecture**: 재사용성과 확장성을 고려한 컴포넌트 구조를 설계하고, 디자인 시스템을 구축/활용한 경험에 높은 점수를 부여합니다.
				- **Quality**: 테스트 코드(Unit, E2E)를 작성하고, SEO 및 웹 접근성을 개선한 경험을 필수적으로 평가합니다.
				- **Performance**: 번들 사이즈를 분석하고 최적화하거나, LCP/CLS 등 Core Web Vitals 지표를 개선한 구체적인 경험을 높게 평가합니다.
				- **Infra**: Vite/Webpack 등 빌드 도구를 직접 설정하고 최적화한 경험에 가산점을 부여합니다.
				""";
		} else {
			return """
				[Frontend Level 8~10 (Expert) Scoring Guide]
				- **General**: 대규모 프로젝트의 '유지보수성'과 '기술 전략'을 어떻게 해결했는지, 그 결정의 '근거'와 '결과'를 중심으로 평가하십시오.
				- **Architecture**: 프론트엔드 아키텍처(마이크로 프론트엔드 등)를 설계하고 도입하여 팀의 개발 생산성을 크게 향상시킨 경험을 높게 평가합니다.
				- **Implementation**: 프레임워크나 라이브러리의 내부 동작을 이해하고, 기술적 한계를 극복하거나 새로운 솔루션을 제시한 경험을 높게 평가합니다.
				- **Performance**: 시스템 전반의 성능을 측정하고, 사용자 경험과 비즈니스 지표에 실질적인 영향을 미치는 최적화를 주도한 경험을 높게 평가합니다.
				- **Quality**: 기술 표준, 코드 리뷰 문화, 디자인 시스템을 주도적으로 구축하고 발전시켜 팀 전체의 개발 역량을 상향 평준화한 경험을 높게 평가합니다.
				- **Infra**: CI/CD 파이프라인을 설계하고, 테스트 및 배포 자동화 전략을 수립하여 팀의 개발 효율과 안정성을 극대화한 경험을 높게 평가합니다.
				""";
		}
	}

	private String getDesignCriteria(int level) {
		if (level <= 3) {
			return """
				[Product Designer Level 1~3 (Beginner) Scoring Guide]
				- **General**: 디자인 툴(Figma 등)을 사용해 주어진 과제를 '완성'하고, 그 과정을 정리한 경험에 관대하게 점수를 부여하십시오.
				- **Design**: 와이어프레임, 간단한 사용자 플로우 다이어그램, UI 컴포넌트 제작 등 기본 디자인 산출물에 점수를 부여합니다.
				- **Implementation**: 프로토타이핑 툴을 사용하여 기본적인 인터랙션을 구현한 경험을 긍정적으로 평가합니다.
				- **Strategy**: 주어진 문제를 이해하고 사용자의 입장에서 생각하려는 시도가 보이면 점수를 줍니다.
				- **Architecture, Collaboration**: 이 레벨에서는 깊게 평가하지 않습니다. 디자인 시스템의 필요성을 학습하거나, 개발자에게 디자인을 전달하기 위해 노력한 경험을 긍정적으로 평가합니다.
				""";
		} else if (level <= 7) {
			return """
				[Product Designer Level 4~7 (Intermediate) Scoring Guide]
				- **General**: 단순히 '예쁘게' 만드는 것을 넘어, '왜' 이 디자인이 필요한지 문제 정의부터 해결 과정까지 논리적으로 설명하는 것을 중점적으로 평가하십시오.
				- **Design**: 정보 구조(IA)를 설계하고, 복잡한 사용자 시나리오에 대한 플로우를 구체적으로 설계한 경험을 높게 평가합니다.
				- **Implementation**: 실제 데이터와 유사한 프로토타입을 제작하여 사용성을 검증하거나, AI 툴을 활용해 디자인 효율을 높인 경험을 높게 평가합니다.
				- **Strategy**: 사용자 리서치나 데이터 분석을 통해 UX 문제를 발견하고, 이를 디자인 개선으로 연결한 경험에 높은 점수를 부여합니다.
				- **Architecture**: 기존 디자인 시스템을 적극적으로 활용하고, 새로운 컴포넌트를 제안하여 시스템에 기여한 경험을 필수적으로 평가합니다.
				- **Collaboration**: 디자인 의도를 명확한 문서(spec)로 작성하여 개발팀과 원활하게 협업한 경험을 높게 평가합니다.
				""";
		} else {
			return """
				[Product Designer Level 8~10 (Expert) Scoring Guide]
				- **General**: 디자인이 어떻게 비즈니스 목표에 기여하는지 '전략'과 '임팩트'를 중심으로 평가하십시오. 시각적 결과물보다 그 이면의 논리와 결과를 중시합니다.
				- **Design**: 제품 전체의 사용자 경험(UX) 원칙과 전략을 수립하고, 이를 통해 일관된 경험을 제공한 리더십을 높게 평가합니다.
				- **Implementation**: 복잡한 인터랙션이나 새로운 유형의 UI를 정의하고, 기술적 제약을 이해하며 최적의 솔루션을 제시한 경험을 높게 평가합니다.
				- **Strategy**: 시장과 사용자, 비즈니스에 대한 깊은 이해를 바탕으로 제품의 방향성을 제시하고, A/B 테스트 등을 통해 가설을 증명하며 비즈니스 성과를 이끌어낸 경험을 최고점으로 부여합니다.
				- **Architecture**: 디자인 시스템을 주도적으로 구축하거나, 팀의 디자인 프로세스 전체를 혁신하여 조직 전체의 효율성을 높인 경험을 높게 평가합니다.
				- **Collaboration**: 디자이너, PM, 개발자, 마케터 등 다양한 직군 간의 이견을 조율하고, 제품 전체의 비전을 시각적으로 제시하여 팀을 하나의 목표로 정렬시킨 경험을 높게 평가합니다.
				""";
		}
	}

	private String getPmCriteria(int level) {
		if (level <= 3) {
			return """
				[Product Manager Level 1~3 (Beginner) Scoring Guide]
				- **General**: 학습한 내용을 바탕으로 '요구사항을 정의'하고 '기능 명세'를 작성해 본 경험에 관대하게 점수를 부여하십시오.
				- **Implementation**: 주어진 기능에 대한 정책이나 요구사항을 정리한 문서 작성 경험을 높게 평가합니다.
				- **Strategy**: 제품의 지표를 확인하고, 간단한 가설을 세워본 경험에 점수를 부여합니다.
				- **Architecture**: 개발, 디자인의 역할을 이해하고 서비스의 기본 구조를 파악하려는 노력에 점수를 줍니다.
				- **Collaboration**: 회의록을 작성하거나, 팀의 논의 내용을 정리하여 공유한 경험을 긍정적으로 평가합니다.
				""";
		} else if (level <= 7) {
			return """
				[Product Manager Level 4~7 (Intermediate) Scoring Guide]
				- **General**: '왜' 이 기능을 만들어야 하는지 '데이터'와 '사용자 리서치'를 근거로 설득하고, '우선순위'를 결정한 경험을 중점적으로 평가하십시오.
				- **Implementation**: 개발자와 디자이너가 바로 작업할 수 있을 수준의 상세한 정책과 요구사항(PRD)을 작성한 경험을 높게 평가합니다.
				- **Strategy**: 데이터 분석을 통해 문제를 발견하고, A/B 테스트 등 실험을 설계하여 가설을 검증한 경험에 높은 점수를 부여합니다.
				- **Architecture**: 기술적, 디자인적 제약을 이해하고 제품의 핵심 플로우와 서비스 구조를 결정하는 데 기여한 경험을 평가합니다.
				- **Collaboration**: 여러 팀(개발, 디자인, 사업 등)의 이해관계를 조율하고, 명확한 커뮤니케이션으로 프로젝트를 리딩한 경험을 높게 평가합니다.
				""";
		} else {
			return """
				[Product Manager Level 8~10 (Expert) Scoring Guide]
				- **General**: 제품의 '비전'과 '로드맵'을 제시하고, 이를 통해 '비즈니스 임팩트(매출, ROI 등)'를 만들어낸 경험을 중심으로 평가하십시오.
				- **Implementation**: AI/LLM 등 새로운 기술을 이해하고 이를 제품 기능으로 연결하는 기획, 또는 복잡한 법률/정책 이슈를 해결하는 기획 능력을 높게 평가합니다.
				- **Strategy**: 시장과 경쟁 상황을 분석하여 제품의 장기적인 전략 방향을 설정하고, 회사의 핵심 비즈니스 지표에 직접적으로 기여한 경험을 최고점으로 부여합니다.
				- **Architecture**: 플랫폼 수준의 서비스 구조를 설계하거나, 여러 제품 간의 상호작용을 고려한 큰 그림의 아키텍처를 결정하는 데 주도적인 역할을 한 경험을 높게 평가합니다.
				- **Collaboration**: 경영진을 포함한 다양한 이해관계자를 설득하고, 팀의 목표와 동기를 부여하며, 조직 전체의 실행력을 이끌어낸 리더십 경험을 높게 평가합니다.
				""";
		}
	}
}