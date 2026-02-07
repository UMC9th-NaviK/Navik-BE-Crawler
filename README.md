# Navik BE Crawler

Spring Boot 기반의 AI 성장 기록 분석 및 채용 크롤링 백엔드 서비스입니다.

---

## 성장 기록 분석 (Growth Analysis)

### 개요

사용자가 제출한 성장 기록(텍스트, Notion 페이지, GitHub PR)을 AI가 직무별 페르소나로 평가하고, 10개 KPI 카드에 대한 점수 변화(delta)를 산출합니다.

### 분석 흐름

```
[Client Request]
       |
       v
 1. 페르소나 로드 (PersonaPromptLoader)
    - base-instruction.txt + personas/{job}.txt
    - 시스템 프롬프트에 KPI 카드 10개 포함
       |
       v
 2. 컨텐츠 타입 판별 (ContentTypeHelper)
    - NOTION_LINK  → fetchNotionPage Tool 추가
    - GITHUB_PR    → fetchGitHubPR Tool 추가
    - TEXT         → 추가 Tool 없음
       |
       v
 3. 유저 프롬프트 구성 (PromptBuilder)
    - user-prompt-template.txt 기반
    - 변수 치환: 컨텐츠, 레벨, 이력서, KPI 포화도
       |
       v
 4. AI 호출 (Spring AI ChatClient + Tool Calling)
    - System: 페르소나 + 평가절차 + KPI 카드
    - User: 분석 대상 + 컨텍스트
    - Tools: retrieveLevelCriteria, (fetchNotionPage | fetchGitHubPR)
       |
       v
 5. 응답 파싱 (ResponseParser)
    - JSON → GrowthAnalysisResponse(title, content, kpis[])
```

### API

```
POST /v1/growth-logs/evaluate/user-input
Content-Type: application/json; charset=UTF-8
```

**Request**
```json
{
  "userId": 1,
  "jobId": 4,
  "levelValue": 5,
  "context": {
    "resumeText": "이력서 요약 텍스트",
    "recentGrowthLogs": [],
    "recentKpiDeltas": [
      { "growthLogId": 1, "kpiCardId": 3, "delta": 5 }
    ],
    "newContent": "분석할 텍스트 또는 Notion/GitHub PR 링크"
  }
}
```

**Response**
```json
{
  "title": "한 줄 요약 제목",
  "content": "분석 결과 및 피드백",
  "kpis": [
    { "kpiCardId": 1, "delta": 5 },
    { "kpiCardId": 6, "delta": 3 }
  ]
}
```

### 프롬프트 구조

```
prompts/growth/
├── base-instruction.txt           # 공통 평가 절차 + 출력 형식(JSON)
├── personas/
│   ├── backend-engineer.txt       # jobId=4 | 역할 정의 + KPI 카드 10개
│   ├── frontend-engineer.txt      # jobId=3 | 역할 정의 + KPI 카드 10개
│   ├── product-designer.txt       # jobId=2 | 역할 정의 + KPI 카드 10개
│   ├── product-manager.txt        # jobId=1 | 역할 정의 + KPI 카드 10개
│   └── default-coach.txt          # fallback 페르소나
├── templates/
│   └── user-prompt-template.txt   # 유저 프롬프트 템플릿 ({CONTENT_SECTION} 등)
└── criteria/                      # 레벨별 평가 기준 (Tool Calling으로 로드)
    ├── backend/
    │   └── level-1.txt ~ level-10.txt
    ├── frontend/
    │   └── level-1.txt ~ level-10.txt
    ├── designer/
    │   └── level-1.txt ~ level-10.txt
    └── pm/
        └── level-1.txt ~ level-10.txt
```

**System Prompt** = `base-instruction.txt` + `personas/{job}.txt`
- 평가 절차, JSON 출력 형식, 역할 정의, KPI 카드 목록 포함

**User Prompt** = `user-prompt-template.txt` (변수 치환)
- 분석 대상 컨텐츠, 레벨, 이력서 요약, KPI 포화도 데이터

### Spring AI Tool Calling

| Tool 이름 | 역할 | 호출 조건 |
|---|---|---|
| `retrieveLevelCriteria` | 직무+레벨별 평가 가이드라인 로드 | 항상 |
| `fetchNotionPage` | Notion 페이지 마크다운 추출 | 컨텐츠가 Notion 링크일 때 |
| `fetchGitHubPR` | GitHub PR 변경사항 추출 | 컨텐츠가 GitHub PR 링크일 때 |

### KPI 포화도 (Saturation)

최근 KPI delta 이력을 집계하여 특정 카드에 점수가 편중되는 것을 방지합니다.
- 동일 KPI 카드 3회 이상 → 패널티 적용

---

## 프로젝트 구조

```
src/main/java/navik/
├── NavikApplication.java
├── HealthCheckController.java          # GET /health
│
├── ai/                                 # AI/LLM 공통 모듈
│   ├── client/
│   │   ├── LLMClient.java             # 채용 분석용 LLM 호출
│   │   └── EmbeddingClient.java
│   ├── config/
│   │   └── ChatClientConfig.java      # Spring AI ChatClient 설정
│   └── util/
│       └── PromptLoader.java          # Resource 기반 프롬프트 로더
│
├── growth/                             # 성장 분석 모듈
│   ├── analysis/
│   │   ├── controller/
│   │   │   └── GrowthAnalysisController.java
│   │   ├── dto/
│   │   │   ├── AnalysisRequest.java
│   │   │   └── AnalysisResponse.java
│   │   ├── service/
│   │   │   ├── GrowthAnalysisService.java      # 분석 오케스트레이션
│   │   │   ├── parser/
│   │   │   │   └── ResponseParser.java         # AI JSON 응답 파싱
│   │   │   ├── prompt/
│   │   │   │   └── PromptBuilder.java          # 유저 프롬프트 구성
│   │   │   └── util/
│   │   │       └── ContentTypeHelper.java      # 컨텐츠 타입 판별 + Tool 목록
│   │   └── strategy/
│   │       └── PersonaPromptLoader.java        # 직무별 페르소나 로드
│   ├── config/
│   │   └── GrowthAnalysisToolConfig.java       # Spring AI Tool Bean 등록
│   ├── extractor/
│   │   ├── NotionPageExtractor.java            # Notion 페이지 마크다운 변환
│   │   └── GitHubPRExtractor.java              # GitHub PR 정보 추출
│   ├── notion/                                 # Notion OAuth 연동
│   │   ├── controller/
│   │   │   └── NotionOAuthController.java
│   │   ├── service/
│   │   │   └── NotionOAuthService.java
│   │   └── api/
│   │       └── NotionApiClient.java
│   └── tool/
│       ├── dto/
│       │   ├── ToolRequests.java
│       │   └── ToolResponses.java
│       └── service/
│           └── LevelCriteriaService.java       # 레벨별 평가 기준 로드
│
├── crawler/                            # 채용 크롤링 모듈
│   ├── controller/
│   │   └── CrawlerController.java      # GET /trigger, /stop, /status
│   ├── service/
│   │   ├── CrawlerService.java
│   │   └── CrawlerControlService.java
│   └── scheduler/
│       └── CrawlerScheduler.java
│
├── ocr/                                # PDF OCR 모듈
│   └── controller/
│       └── OCRController.java          # POST /ocr/pdf
│
└── redis/                              # Redis 연동
    └── client/
        └── RedisStreamProducer.java
```

---

## 기술 스택

- **Framework**: Spring Boot, Spring AI
- **AI**: Spring AI ChatClient (Function/Tool Calling)
- **External API**: Notion API (OAuth), GitHub REST API, Naver Clova OCR
- **Infra**: Redis
- **Build**: Gradle
