# 노션 연동 성장 기록 분석 시스템 구현 가이드 (MVP)

## 📋 개요

사용자가 공부하면서 작성한 **공개 노션 페이지**와 **GitHub PR**을 분석하여 직무별 성장 기록을 자동으로 생성하는 시스템입니다.

### 핵심 전략: Spring AI Native Function Calling

- ❌ MCP 서버 불필요
- ❌ Notion API Token 불필요 (공개 페이지만 지원)
- ✅ Spring AI의 `@Tool` 또는 Function 방식 활용
- ✅ Claude가 직접 Tool을 호출하여 컨텐츠 수집 및 분석
- ✅ 간단하고 빠른 구현

---

## 🎯 MVP 범위

### 지원 기능
1. **공개 노션 페이지 분석**
    - 공개 URL만 지원 (예: `https://notion.so/xxxxx`)
    - Notion API 또는 HTML 파싱으로 컨텐츠 추출
    - 직무별 KPI 관점 분석

2. **GitHub Public PR 분석**
    - 공개 PR만 지원 (예: `https://github.com/user/repo/pull/123`)
    - GitHub REST API로 PR 정보 수집
    - 코드 변경사항 및 기여도 분석

3. **AI 자동 분석**
    - Claude가 Tool을 통해 컨텐츠 수집
    - 직무 및 KPI 카드 기반 요약/피드백 생성
    - 성장 점수 자동 산정

### 제외 사항 (Phase 2 이후)
- Private 노션 페이지 (API Token 필요)
- Private GitHub Repository
- 노션 Database 연동
- 일괄 분석 기능

---

## 🏗️ 시스템 아키텍처

```
[사용자 앱]
    ↓ (공개 URL 전송)
[NaviK BE - Spring Boot]
    ↓ (분석 요청 + KPI Context)
[Spring AI 서버]
    - Claude API 호출
    - Claude가 Tools 자동 실행:
      → fetchNotionPage(url)
      → fetchGitHubPR(url)
    - AI가 컨텐츠 분석 후 응답
    ↓ (분석 결과 반환)
[NaviK BE]
    - GrowthLog 저장
    - KPI 점수 업데이트
```
 
---

## 🛠️ 기술 스택

### Spring AI 서버

```gradle
dependencies {
    // Spring AI (Anthropic)
    implementation 'org.springframework.ai:spring-ai-anthropic-spring-boot-starter:1.0.0-M4'
 
    // Web Client (HTTP 요청)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
 
    // JSON 파싱
    implementation 'com.fasterxml.jackson.core:jackson-databind'
 
    // HTML 파싱 (노션 공개 페이지)
    implementation 'org.jsoup:jsoup:1.17.2'
}
```

### 필요 자원

| 자원 | 용도 | 비용 | 획득 방법 |
|------|------|------|----------|
| **Anthropic API Key** | Claude API 호출 | 종량제<br>($3/1M input tokens) | [console.anthropic.com](https://console.anthropic.com) |
| ~~Notion API Token~~ | ❌ 불필요 (공개 페이지만) | - | - |
| ~~GitHub Token~~ | ❌ 불필요 (Public PR만) | - | - |
 
---

## 📦 데이터베이스 스키마 확장

### GrowthLog 엔티티 수정

```java
@Entity
@Table(name = "growth_logs")
public class GrowthLog extends BaseEntity {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_card_id", nullable = false)
    private KpiCard kpiCard;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private GrowthType type;  // NOTION 또는 GITHUB_PR 추가
 
    @Column(name = "title", nullable = false)
    private String title;
 
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
 
    @Column(name = "score", nullable = false)
    private Integer score;
 
    // ⬇️ 새로 추가되는 필드
    @Column(name = "source_url")
    private String sourceUrl;  // 노션 URL 또는 GitHub PR URL
 
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;  // AI 생성 요약
 
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;  // AI 생성 피드백
}
```

### GrowthType Enum 확장

```java
public enum GrowthType {
    PORTFOLIO,      // 기존: 포트폴리오
    USER_INPUT,     // 기존: 텍스트 입력
    FEEDBACK,       // 기존: 스터디 피드백
    NOTION,         // ⬅️ 새로 추가
    GITHUB_PR       // ⬅️ 새로 추가
}
```

### DDL 마이그레이션

```sql
-- GrowthLog 테이블 확장
ALTER TABLE growth_logs
ADD COLUMN user_id BIGINT REFERENCES users(id),
ADD COLUMN source_url TEXT,
ADD COLUMN ai_summary TEXT,
ADD COLUMN ai_feedback TEXT,
ALTER COLUMN content TYPE TEXT;
 
-- 인덱스 추가
CREATE INDEX idx_growth_logs_user_type ON growth_logs(user_id, type);
CREATE INDEX idx_growth_logs_source_url ON growth_logs(source_url);
 
-- user_id NOT NULL 제약 조건 (기존 데이터 처리 후)
-- ALTER TABLE growth_logs ALTER COLUMN user_id SET NOT NULL;
```
 
---

## 🔧 Spring AI 서버 구현

### 1. application.yml

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-3-5-sonnet-20241022
          max-tokens: 4096
          temperature: 0.3
 
notion:
  public-api:
    enabled: true
    timeout: 10000  # 10초
 
github:
  public-api:
    enabled: true
    base-url: https://api.github.com
    timeout: 10000
```

### 2. Native Function (Tool) 정의

Spring AI는 두 가지 방식으로 Tool을 정의할 수 있습니다:

#### 방식 A: Function Bean 등록 (권장)

```java
@Configuration
public class GrowthAnalysisToolConfig {
 
    @Bean
    @Description("공개 노션 페이지의 전체 컨텐츠를 추출합니다.")
    public Function<NotionPageRequest, String> fetchNotionPage(NotionPageExtractor extractor) {
        return request -> {
            try {
                return extractor.extractPublicPage(request.url());
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        };
    }
 
    @Bean
    @Description("GitHub Public PR의 변경사항과 설명을 추출합니다.")
    public Function<GitHubPRRequest, String> fetchGitHubPR(GitHubPRExtractor extractor) {
        return request -> {
            try {
                return extractor.extractPublicPR(request.url());
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        };
    }
 
    // Tool 입력 DTO
    public record NotionPageRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("노션 공개 페이지 URL")
        String url
    ) {}
 
    public record GitHubPRRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("GitHub Pull Request URL")
        String url
    ) {}
}
```

#### 방식 B: @Tool 어노테이션 (Spring AI 최신 버전)

```java
@Component
public class GrowthAnalysisTools {
 
    private final NotionPageExtractor notionExtractor;
    private final GitHubPRExtractor githubExtractor;
 
    @Tool(description = "공개 노션 페이지의 전체 컨텐츠를 추출합니다.")
    public String fetchNotionPage(
        @ToolParam(description = "노션 공개 페이지 URL") String url
    ) {
        return notionExtractor.extractPublicPage(url);
    }
 
    @Tool(description = "GitHub Public PR의 변경사항과 설명을 추출합니다.")
    public String fetchGitHubPR(
        @ToolParam(description = "GitHub Pull Request URL") String url
    ) {
        return githubExtractor.extractPublicPR(url);
    }
}
```

### 3. 노션 페이지 추출기 (JSoup 사용)

```java
@Component
@RequiredArgsConstructor
public class NotionPageExtractor {
 
    private final WebClient webClient = WebClient.builder()
        .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
        .build();
 
    /**
     * 공개 노션 페이지에서 컨텐츠 추출
     * HTML 파싱 방식 (API Token 불필요)
     */
    public String extractPublicPage(String url) {
        try {
            // 1. HTML 다운로드
            String html = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
 
            if (html == null) {
                throw new IllegalStateException("Failed to fetch Notion page");
            }
 
            // 2. JSoup으로 파싱
            Document doc = Jsoup.parse(html);
 
            // 3. 제목 추출
            String title = doc.select("div.notion-title").text();
            if (title.isEmpty()) {
                title = doc.title();
            }
 
            // 4. 본문 추출 (notion-page-content 클래스)
            Element content = doc.selectFirst("div.notion-page-content");
 
            if (content == null) {
                throw new IllegalStateException("Cannot find notion content");
            }
 
            // 5. 텍스트 정제
            String text = content.text();
 
            // 6. 마크다운 형식으로 변환 (간단 버전)
            StringBuilder markdown = new StringBuilder();
            markdown.append("# ").append(title).append("\n\n");
 
            // 각 블록 처리
            content.select("div[data-block-id]").forEach(block -> {
                String blockText = block.text().trim();
                if (!blockText.isEmpty()) {
                    // 헤딩 처리
                    if (block.hasClass("notion-header-block")) {
                        markdown.append("## ").append(blockText).append("\n\n");
                    }
                    // 리스트 처리
                    else if (block.hasClass("notion-bulleted_list-block")) {
                        markdown.append("- ").append(blockText).append("\n");
                    }
                    // 일반 텍스트
                    else {
                        markdown.append(blockText).append("\n\n");
                    }
                }
            });
 
            return markdown.toString();
 
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract Notion page: " + url, e);
        }
    }
}
```

### 4. GitHub PR 추출기

```java
@Component
@RequiredArgsConstructor
public class GitHubPRExtractor {
 
    private final WebClient webClient = WebClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
        .build();
 
    /**
     * GitHub Public PR 정보 추출
     * 예시 URL: https://github.com/owner/repo/pull/123
     */
    public String extractPublicPR(String url) {
        try {
            // URL 파싱: https://github.com/{owner}/{repo}/pull/{number}
            String[] parts = url.replace("https://github.com/", "").split("/");
            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid GitHub PR URL");
            }
 
            String owner = parts[0];
            String repo = parts[1];
            String prNumber = parts[3];
 
            // 1. PR 기본 정보 조회
            GitHubPR pr = webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", owner, repo, prNumber)
                .retrieve()
                .bodyToMono(GitHubPR.class)
                .block();
 
            if (pr == null) {
                throw new IllegalStateException("Failed to fetch GitHub PR");
            }
 
            // 2. PR Files 조회 (변경된 파일 목록)
            GitHubPRFile[] files = webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}/files", owner, repo, prNumber)
                .retrieve()
                .bodyToMono(GitHubPRFile[].class)
                .block();
 
            // 3. 마크다운 형식으로 변환
            StringBuilder markdown = new StringBuilder();
            markdown.append("# PR: ").append(pr.title()).append("\n\n");
            markdown.append("**Author:** ").append(pr.user().login()).append("\n");
            markdown.append("**State:** ").append(pr.state()).append("\n");
            markdown.append("**Created:** ").append(pr.createdAt()).append("\n\n");
 
            if (pr.body() != null && !pr.body().isEmpty()) {
                markdown.append("## Description\n\n");
                markdown.append(pr.body()).append("\n\n");
            }
 
            if (files != null && files.length > 0) {
                markdown.append("## Changed Files\n\n");
                for (GitHubPRFile file : files) {
                    markdown.append("- **").append(file.filename()).append("**\n");
                    markdown.append("  - Additions: ").append(file.additions()).append("\n");
                    markdown.append("  - Deletions: ").append(file.deletions()).append("\n");
 
                    // Patch 내용 (diff) - 길이 제한
                    if (file.patch() != null && file.patch().length() < 2000) {
                        markdown.append("  ```diff\n");
                        markdown.append("  ").append(file.patch()).append("\n");
                        markdown.append("  ```\n");
                    }
                    markdown.append("\n");
                }
            }
 
            markdown.append("**Total Changes:** +").append(pr.additions())
                    .append(" -").append(pr.deletions()).append("\n");
 
            return markdown.toString();
 
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract GitHub PR: " + url, e);
        }
    }
 
    // DTO
    private record GitHubPR(
        String title,
        String body,
        String state,
        User user,
        String createdAt,
        int additions,
        int deletions
    ) {}
 
    private record User(String login) {}
 
    private record GitHubPRFile(
        String filename,
        int additions,
        int deletions,
        String patch
    ) {}
}
```

### 5. AI 분석 서비스

```java
@Service
@RequiredArgsConstructor
public class GrowthAnalysisService {
 
    private final AnthropicChatModel chatModel;
    private final FunctionCallbackContext functionCallbackContext;
 
    public GrowthAnalysisResponse analyze(GrowthAnalysisRequest request) {
 
        // 1. 시스템 프롬프트 생성
        String systemPrompt = buildSystemPrompt(request.jobContext());
 
        // 2. 사용자 프롬프트 생성
        String userPrompt = buildUserPrompt(request);
 
        // 3. Claude API 호출 (Tool 포함)
        ChatResponse response = chatModel.call(
            new Prompt(
                List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
                ),
                AnthropicChatOptions.builder()
                    .maxTokens(4096)
                    .temperature(0.3)
                    .functions(Set.of("fetchNotionPage", "fetchGitHubPR"))  // Tool 등록
                    .build()
            )
        );
 
        // 4. 응답 파싱
        return parseResponse(response);
    }
 
    private String buildSystemPrompt(JobContext jobContext) {
        return """
            당신은 %s 직무의 성장 분석 전문가입니다.
 
            분석 대상 KPI: %s
            - 강점: %s
            - 약점: %s
 
            사용자가 제공한 URL의 학습 기록을 분석하여 다음을 제공해주세요:
 
            1. 핵심 내용 요약 (500자 이내)
               - 학습한 기술/개념
               - 수행한 작업
               - 주요 성과
 
            2. KPI 관점의 구체적 피드백 (1000자 이내)
               - 강점 관점에서 잘한 점
               - 약점 관점에서 보완할 점
               - 다음 학습 방향 제안
 
            3. 학습 성과 점수 (0-20점)
               - 0-5점: 기초 개념 학습
               - 6-10점: 실습 및 적용
               - 11-15점: 심화 학습 및 응용
               - 16-20점: 독창적 문제 해결 또는 프로덕션 적용
 
            응답 형식은 반드시 다음 JSON 구조를 따라주세요:
            {
              "title": "추출된 제목",
              "summary": "요약 내용",
              "feedback": "피드백 내용",
              "score": 15
            }
 
            **중요:**
            - URL 내용을 확인하려면 제공된 Tool을 사용하세요:
              - 노션 페이지: fetchNotionPage(url)
              - GitHub PR: fetchGitHubPR(url)
            - Tool을 먼저 호출하여 컨텐츠를 가져온 후 분석하세요.
            """.formatted(
                jobContext.jobName(),
                jobContext.kpiCardName(),
                jobContext.strongTitle(),
                jobContext.weakTitle()
            );
    }
 
    private String buildUserPrompt(GrowthAnalysisRequest request) {
        return """
            다음 URL의 학습 기록을 분석해주세요:
            %s
 
            직무: %s
            KPI 카드: %s
 
            위 URL의 내용을 먼저 Tool을 사용해 가져온 후,
            '%s' KPI 카드 관점에서 분석해주세요.
            """.formatted(
                request.sourceUrl(),
                request.jobContext().jobName(),
                request.jobContext().kpiCardName(),
                request.jobContext().kpiCardName()
            );
    }
 
    private GrowthAnalysisResponse parseResponse(ChatResponse response) {
        String content = response.getResult().getOutput().getContent();
 
        // JSON 파싱
        try {
            ObjectMapper mapper = new ObjectMapper();
 
            // JSON 블록 추출 (```json ... ``` 제거)
            String json = content;
            if (content.contains("```json")) {
                json = content.substring(
                    content.indexOf("```json") + 7,
                    content.lastIndexOf("```")
                ).trim();
            } else if (content.contains("```")) {
                json = content.substring(
                    content.indexOf("```") + 3,
                    content.lastIndexOf("```")
                ).trim();
            }
 
            JsonNode node = mapper.readTree(json);
 
            return GrowthAnalysisResponse.builder()
                .title(node.get("title").asText())
                .summary(node.get("summary").asText())
                .feedback(node.get("feedback").asText())
                .score(node.get("score").asInt())
                .build();
 
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }
}
```

### 6. DTO 정의

```java
// 분석 요청
public record GrowthAnalysisRequest(
    String sourceUrl,        // 노션 또는 GitHub PR URL
    JobContext jobContext    // 직무 및 KPI 정보
) {}
 
public record JobContext(
    String jobName,
    String kpiCardName,
    String strongTitle,
    String weakTitle
) {}
 
// 분석 응답
@Builder
public record GrowthAnalysisResponse(
    String title,
    String summary,
    String feedback,
    Integer score
) {}
```

### 7. REST API Controller

```java
@RestController
@RequestMapping("/api/ai/growth")
@RequiredArgsConstructor
public class GrowthAnalysisController {
 
    private final GrowthAnalysisService analysisService;
 
    @PostMapping("/analyze")
    public ResponseEntity<GrowthAnalysisResponse> analyzeGrowthLog(
        @RequestBody @Valid GrowthAnalysisRequest request
    ) {
        GrowthAnalysisResponse response = analysisService.analyze(request);
        return ResponseEntity.ok(response);
    }
}
```
 
---

## 🔌 NaviK BE 통합

### 1. DTO

```java
// 요청 DTO
public record GrowthLogAnalysisRequestDTO(
    @NotBlank String sourceUrl,
    @NotNull Long kpiCardId
) {}
 
// 응답 DTO
@Builder
public record GrowthLogAnalysisResponseDTO(
    Long growthLogId,
    String title,
    String summary,
    String feedback,
    Integer score,
    String sourceUrl,
    LocalDateTime analyzedAt
) {
    public static GrowthLogAnalysisResponseDTO from(GrowthLog growthLog) {
        return GrowthLogAnalysisResponseDTO.builder()
            .growthLogId(growthLog.getId())
            .title(growthLog.getTitle())
            .summary(growthLog.getAiSummary())
            .feedback(growthLog.getAiFeedback())
            .score(growthLog.getScore())
            .sourceUrl(growthLog.getSourceUrl())
            .analyzedAt(growthLog.getCreatedAt())
            .build();
    }
}
```

### 2. Spring AI 클라이언트

```java
@Component
@RequiredArgsConstructor
public class SpringAiClient {
 
    private final WebClient webClient;
 
    @Value("${spring-ai-server.base-url}")
    private String baseUrl;  // 예: http://localhost:8081
 
    public GrowthAnalysisResponse analyzeGrowthLog(
        String sourceUrl,
        JobContext jobContext
    ) {
        return webClient.post()
            .uri(baseUrl + "/api/ai/growth/analyze")
            .bodyValue(new GrowthAnalysisRequest(sourceUrl, jobContext))
            .retrieve()
            .bodyToMono(GrowthAnalysisResponse.class)
            .block();
    }
}
```

### 3. 서비스

```java
@Service
@RequiredArgsConstructor
@Transactional
public class GrowthLogAnalysisService {
 
    private final GrowthLogRepository growthLogRepository;
    private final KpiCardRepository kpiCardRepository;
    private final KpiScoreIncrementService kpiScoreIncrementService;
    private final SpringAiClient springAiClient;
 
    public GrowthLogAnalysisResponseDTO analyzeAndSave(
        Long userId,
        GrowthLogAnalysisRequestDTO request
    ) {
        // 1. KPI 카드 조회
        KpiCard kpiCard = kpiCardRepository.findByIdWithJob(request.kpiCardId())
            .orElseThrow(() -> new NotFoundException("KPI 카드를 찾을 수 없습니다."));
 
        // 2. URL 타입 판별
        GrowthType growthType = determineGrowthType(request.sourceUrl());
 
        // 3. Spring AI 서버에 분석 요청
        JobContext jobContext = JobContext.builder()
            .jobName(kpiCard.getJob().getName())
            .kpiCardName(kpiCard.getName())
            .strongTitle(kpiCard.getStrongTitle())
            .weakTitle(kpiCard.getWeakTitle())
            .build();
 
        GrowthAnalysisResponse aiResponse =
            springAiClient.analyzeGrowthLog(request.sourceUrl(), jobContext);
 
        // 4. GrowthLog 저장
        GrowthLog growthLog = GrowthLog.builder()
            .userId(userId)
            .kpiCard(kpiCard)
            .type(growthType)
            .title(aiResponse.title())
            .content(aiResponse.summary())  // 요약을 content에 저장
            .score(aiResponse.score())
            .sourceUrl(request.sourceUrl())
            .aiSummary(aiResponse.summary())
            .aiFeedback(aiResponse.feedback())
            .build();
 
        growthLog = growthLogRepository.save(growthLog);
 
        // 5. KPI 점수 증가
        kpiScoreIncrementService.incrementKpiScore(
            userId,
            request.kpiCardId(),
            aiResponse.score()
        );
 
        return GrowthLogAnalysisResponseDTO.from(growthLog);
    }
 
    private GrowthType determineGrowthType(String url) {
        if (url.contains("notion.so") || url.contains("notion.site")) {
            return GrowthType.NOTION;
        } else if (url.contains("github.com") && url.contains("/pull/")) {
            return GrowthType.GITHUB_PR;
        } else {
            throw new IllegalArgumentException("지원하지 않는 URL 형식입니다.");
        }
    }
}
```

### 4. 컨트롤러

```java
@RestController
@RequestMapping("/v1/growth-logs")
@RequiredArgsConstructor
public class GrowthLogController {
 
    private final GrowthLogAnalysisService analysisService;
 
    @PostMapping("/analyze")
    public ResponseEntity<GrowthLogAnalysisResponseDTO> analyzeGrowthLog(
        @AuthUser Long userId,
        @RequestBody @Valid GrowthLogAnalysisRequestDTO request
    ) {
        GrowthLogAnalysisResponseDTO response =
            analysisService.analyzeAndSave(userId, request);
 
        return ResponseEntity.ok(response);
    }
}
```
 
---

## 📊 API 사용 예시

### 요청

```http
POST /v1/growth-logs/analyze
Content-Type: application/json
Authorization: Bearer {token}
 
{
  "sourceUrl": "https://www.notion.so/my-db-study-123456",
  "kpiCardId": 5
}
```

### 응답

```json
{
  "growthLogId": 123,
  "title": "DB 스터디 2차 - Redis 캐싱 전략 학습",
  "summary": "Redis의 캐싱 전략을 학습하고 실습 프로젝트에 적용했습니다. Cache-Aside 패턴과 Write-Through 패턴의 차이를 이해하고, TTL 설정을 통한 메모리 관리 방법을 익혔습니다.",
  "feedback": "✅ 강점: Redis 기본 개념을 잘 이해하고 실습에 적용했습니다.\n⚠️ 개선점: 분산 환경에서의 캐시 일관성 문제에 대한 추가 학습이 필요합니다.\n📚 다음 학습: Redis Cluster, Sentinel을 활용한 고가용성 구성을 학습해보세요.",
  "score": 12,
  "sourceUrl": "https://www.notion.so/my-db-study-123456",
  "analyzedAt": "2024-11-23T10:30:00"
}
```
 
---

## 🚀 구현 단계

### Week 1: Spring AI 서버 구축
```
✅ Day 1-2: 프로젝트 셋업 및 의존성 추가
✅ Day 3-4: NotionPageExtractor 구현 및 테스트
✅ Day 5-6: GitHubPRExtractor 구현 및 테스트
✅ Day 7: Tool 정의 및 통합 테스트
```

### Week 2: AI 분석 로직 및 BE 통합
```
✅ Day 8-9: GrowthAnalysisService 구현
✅ Day 10-11: 프롬프트 튜닝 및 응답 파싱
✅ Day 12-13: NaviK BE 연동 (API 클라이언트, 서비스)
✅ Day 14: 통합 테스트 및 버그 수정
```

### Week 3: GrowthLog 확장 및 배포
```
✅ Day 15-16: DB 마이그레이션 및 GrowthLog 엔티티 수정
✅ Day 17-18: API 문서화 (Swagger)
✅ Day 19-20: 배포 및 모니터링
✅ Day 21: QA 및 피드백 반영
```
 
---

## 💰 예상 비용

### Claude API (Anthropic)

| 사용량 | Input Tokens | Output Tokens | 비용 |
|--------|-------------|---------------|------|
| 분석 1회 | ~5,000 | ~1,500 | $0.037 |
| 100명 × 10회/월 | 5M | 1.5M | **$37.50/월** |
| 1,000명 × 10회/월 | 50M | 15M | **$375/월** |

### 비용 절감 전략
1. **캐싱**: 같은 URL 재분석 시 캐시 결과 반환
2. **Rate Limiting**: 사용자당 일일 분석 횟수 제한
3. **배치 분석**: 여러 URL을 한 번에 분석 (향후 기능)

---

## ⚠️ 제한사항 및 고려사항

### 공개 URL만 지원
- ✅ 공개 노션 페이지
- ✅ Public GitHub Repository PR
- ❌ Private 노션 페이지 (Phase 2)
- ❌ Private GitHub Repository (Phase 2)

### 노션 HTML 파싱 한계
- 노션이 HTML 구조를 변경하면 파싱 로직 수정 필요
- **대안**: Notion API 사용 (Phase 2에서 Token 기반 인증 추가)

### GitHub API Rate Limit
- **Public API**: 60 requests/hour (인증 없음)
- **Authenticated API**: 5,000 requests/hour (Token 사용 시)
- **해결**: 초기에는 Public API 사용, 이후 GitHub OAuth 추가

---

## 🔐 보안 고려사항

```yaml
보안 체크리스트:
  ✅ URL 유효성 검증 (허용된 도메인만)
  ✅ SSRF 방지 (내부 IP 차단)
  ✅ XSS 방지 (AI 응답 sanitize)
  ✅ Rate Limiting (사용자별, IP별)
  ✅ API Key 암호화 저장
  ✅ 로그에 민감 정보 미포함
```
 
---

## 📈 향후 확장 계획 (Phase 2)

### Private 컨텐츠 지원
- Notion OAuth 2.0 연동
- GitHub OAuth 연동
- 사용자별 Token 관리

### 고급 기능
- 여러 URL 일괄 분석
- 주간/월간 학습 리포트 자동 생성
- AI 피드백 기반 학습 경로 추천
- 노션 Database 연동 (스터디 일지 자동 수집)

### 성능 최적화
- Redis 캐싱
- 비동기 분석 처리 (RabbitMQ)
- 분석 진행률 실시간 표시 (SSE)

---

## 🎯 성공 지표 (KPI)

| 지표 | 목표 | 측정 방법 |
|------|------|----------|
| 분석 성공률 | > 95% | 성공/전체 요청 |
| 평균 응답 시간 | < 15초 | API 응답 시간 |
| 사용자 만족도 | > 4.0/5.0 | 피드백 품질 평가 |
| 월간 활성 사용자 | 100명+ | 분석 API 호출 사용자 수 |
 
---

## 📚 참고 자료

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Anthropic API Reference](https://docs.anthropic.com/en/api/getting-started)
- [Notion API](https://developers.notion.com/)
- [GitHub REST API](https://docs.github.com/en/rest)
- [JSoup Documentation](https://jsoup.org/)

---

**문서 버전**: 1.0 MVP
**작성일**: 2024-01-24
**담당자**: NaviK Development Team