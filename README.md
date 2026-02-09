# Navik BE Crawler

> Na:viK 메인 서버에서 AI 분석, 채용 크롤링, PDF OCR 등 연산 집약적 기능을 분리한 보조 서버입니다.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-blue)

---

## 개요

메인 서버가 사용자 인증, 성장 기록 CRUD, KPI 관리 등 핵심 비즈니스 로직을 담당하고, 이 서버는 **AI 추론**과 **외부 데이터 수집**에 집중합니다.

```
┌──────────────────────────────┐                 ┌──────────────────────────────┐
│        Na:viK Main           │    REST API     │        Na:viK Crawler        │
│           Server             │───────────────▶│          (This Server)       │
│                              │                 │                              │
│  • Auth / Authorization      │◀───────────────│  • AI Growth Analysis        │
│  • Growth Logs               │    JSON Resp    │  • Job Posting Crawler       │
│  • KPI Management            │                 │  • PDF OCR                   │
│                              │◀─ ─ ─ ─ ─ ─ ─ ─│                              │
│                              │  Redis Stream   │                              │
└──────────────────────────────┘                 └──────────────────────────────┘
```

### 이 서버가 담당하는 기능

| 모듈 | 설명 |
|------|------|
| **AI 성장 분석** | 성장 기록(텍스트, Notion, GitHub PR)을 직무별 페르소나로 평가, KPI 점수 산출 |
| **채용 크롤링** | IT 직군 채용 공고를 수집하여 Redis Stream으로 메인 서버에 전달 |
| **PDF OCR** | Naver Clova OCR 기반 PDF 텍스트 추출 (이력서 등록 등) |

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Framework** | Spring Boot 3.5.9, Spring AI 1.1.2 |
| **Language** | Java 21 |
| **AI** | Spring AI ChatClient + Tool Calling (GPT-4.1-mini) |
| **External API** | Notion API v1, GitHub REST API v3, Naver Clova OCR |
| **Infra** | Redis (Stream) |
| **HTTP Client** | Spring WebFlux (WebClient) |
| **Build** | Gradle |
| **기타** | Selenium, JSoup, Lombok |

---

## Getting Started

### 요구사항

- **Java 21** 이상
- **Gradle** 8.x (또는 프로젝트 내 `gradlew` 사용)
- **Redis** 6.x 이상
- **OpenAI API Key**

### 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다. (`.gitignore`에 추가 필수)

```dotenv
# === 필수 ===
SPRING_PROFILES_ACTIVE=dev
OPENAI_API_KEY=sk-your-openai-api-key
REDIS_HOST=localhost
REDIS_PORT=6379
CRAWL_STREAM_KEY=stream
JWT_SECRET=your-jwt-secret

# === 선택 ===
# Naver Clova OCR
NAVER_OCR_URL=https://your-ocr-endpoint
NAVER_OCR_SECRET_KEY=your-ocr-secret

# Notion
NOTION_OAUTH_CLIENT_ID=your-notion-client-id
NOTION_OAUTH_CLIENT_SECRET=your-notion-secret
NOTION_OAUTH_REDIRECT_URI=http://localhost:8080/api/notion/oauth/callback

# AWS
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=your-bucket-name
```

### 실행

```bash
# 개발 실행
./gradlew bootRun

# 빌드 후 JAR 실행
./gradlew build
java -jar build/libs/navik-1.0.0.jar

# 헬스체크
curl http://localhost:8080/health
```

---

## 성장 기록 분석 (Growth Analysis)

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

**jobId 매핑**

| jobId | 직무 | 페르소나 |
|-------|------|---------|
| 1 | PM | `product-manager.txt` |
| 2 | 디자이너 | `product-designer.txt` |
| 3 | 프론트엔드 | `frontend-engineer.txt` |
| 4 | 백엔드 | `backend-engineer.txt` |

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

## API Reference

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/v1/growth-logs/evaluate/user-input` | AI 성장 기록 분석 |
| `GET` | `/health` | 헬스체크 |
| `GET` | `/trigger` | 크롤러 수동 트리거 |
| `GET` | `/stop` | 크롤러 중지 |
| `GET` | `/status` | 크롤러 상태 조회 |
| `POST` | `/ocr/pdf` | PDF 텍스트 추출 (OCR) |

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
│   ├── notion/                                 # Notion API 연동
│   │   ├── api/
│   │   │   └── NotionApiClient.java
│   │   └── config/
│   │       └── NotionWebClientConfig.java
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

## 테스트

```bash
./gradlew test
```

---

## 배포

운영 환경에서는 `.env` 대신 시스템 환경변수 또는 시크릿 매니저를 사용합니다.

```bash
# 필수 환경변수
SPRING_PROFILES_ACTIVE=prod
OPENAI_API_KEY=sk-...
REDIS_HOST=your-redis-host
REDIS_PORT=6379
CRAWL_STREAM_KEY=stream

# 빌드 및 실행
./gradlew build
java -jar build/libs/navik-1.0.0.jar
```

---

## Troubleshooting

| 증상 | 원인 | 해결 |
|------|------|------|
| `Connection refused: localhost:6379` | Redis 미실행 | `redis-server` 실행 또는 Docker로 기동 |
| AI 응답이 비어있음 | OpenAI API Key 미설정/만료 | `.env`에 유효한 `OPENAI_API_KEY` 설정 |
| Notion 페이지 추출 실패 | Integration 미연결 | Notion에서 Integration 연결 후 Access Token 확인 |
| GitHub PR 추출 실패 | Private 저장소 | 현재 Public PR만 지원 |

## 👤 Na:viK BE

| <img src="https://avatars.githubusercontent.com/u/186535028?v=4" width="150" height="150"/> | <img src="https://avatars.githubusercontent.com/u/81423073?v=4" width="150" height="150"/> | <img src="https://avatars.githubusercontent.com/u/81312085?v=4" width="150" height="150"/> | <img src="https://avatars.githubusercontent.com/u/158552165?v=4" width="150" height="150"/> | <img src="https://avatars.githubusercontent.com/u/108278044?v=4" width="150" height="150"/> |
| --- | --- | --- | --- | --- |
| @kjhh2605<br/>[GitHub](https://github.com/kjhh2605) | @bmh7190<br/>[GitHub](https://github.com/bmh7190) | @kfdsy0103<br/>[GitHub](https://github.com/kfdsy0103) | @hardwoong<br/>[GitHub](https://github.com/hardwoong) | @LeeJaeJun1<br/>[GitHub](https://github.com/LeeJaeJun1) 


---

## Support

- **Issues**: [GitHub Issues](https://github.com/UMC9th-NaviK/NaviK-BE-Crawler/issues)
- **Organization**: [UMC 9th - NaviK Team](https://github.com/UMC9th-NaviK)

---

## Credits

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring AI](https://docs.spring.io/spring-ai/reference/) - AI 통합 프레임워크 (Tool Calling)
- [OpenAI API](https://platform.openai.com/) (GPT-4.1-mini, text-embedding-3-small)
- [Notion API](https://developers.notion.com/) - 페이지 데이터 추출
- [GitHub REST API](https://docs.github.com/en/rest) - PR 데이터 추출
- [Naver Clova OCR](https://www.ncloud.com/product/aiService/ocr) - PDF 텍스트 추출
