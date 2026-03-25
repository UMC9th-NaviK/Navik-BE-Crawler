package navik.crawler.service;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import navik.ai.client.EmbeddingClient;
import navik.ai.client.LLMClient;
import navik.ai.dto.LLMResponseDTO;
import navik.ai.enums.AreaType;
import navik.ai.enums.CompanySize;
import navik.ai.enums.EducationLevel;
import navik.ai.enums.EmploymentType;
import navik.ai.enums.ExperienceType;
import navik.ai.enums.IndustryType;
import navik.ai.enums.JobType;
import navik.ai.enums.MajorType;
import navik.crawler.dto.Recruitment;
import navik.crawler.util.CrawlerDataExtractor;
import navik.crawler.util.CrawlerValidator;
import navik.redis.client.RedisStreamProducer;
import navik.redis.config.RedisTestContainersConfig;
import navik.redis.congestion.RedisCongestionManager;

@SpringBootTest
class CrawlerServiceTest extends RedisTestContainersConfig {

	@Autowired
	private CrawlerService crawlerService;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private CrawlerDataExtractor crawlerDataExtractor;
	@MockitoBean
	private CrawlerValidator crawlerValidator;
	@MockitoBean
	private LLMClient llmClient;
	@MockitoBean
	private EmbeddingClient embeddingClient;
	@MockitoBean
	private RedisStreamProducer redisStreamProducer;
	@MockitoBean
	private WebDriverWait wait;

	@MockitoSpyBean
	private RedisCongestionManager redisCongestionManager;

	private final String TEST_STREAM_KEY = "test-stream";
	private final String TEST_GROUP_NAME = "test-group";

	@BeforeEach
	void init() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

		ReflectionTestUtils.setField(crawlerService, "recruitmentStreamKey", TEST_STREAM_KEY);
		ReflectionTestUtils.setField(crawlerService, "recruitmentGroupName", TEST_GROUP_NAME);

		ReflectionTestUtils.setField(redisCongestionManager, "maxMemoryUsage", 0.7);
		ReflectionTestUtils.setField(redisCongestionManager, "maxStreamLength", 10L);
		ReflectionTestUtils.setField(redisCongestionManager, "maxPendingThreshold", 3L);
	}

	@Test
	@DisplayName("processETL() - Redis가 혼잡하면, 대기 로직이 동작하고 해제되면 성공적으로 발행한다.")
	void processETL_ShouldWait_WhenRedisInCongestion() {
		// given
		final int EXPECTED_TIMES = 3;
		setupDefaultMocks();
		doReturn(true, true, false).when(redisCongestionManager).isCongested(TEST_STREAM_KEY, TEST_GROUP_NAME);

		// when
		crawlerService.processETL(wait);

		// then
		verify(redisCongestionManager, times(EXPECTED_TIMES)).isCongested(TEST_STREAM_KEY, TEST_GROUP_NAME);
		verify(redisStreamProducer, times(1)).produceRecruitment(eq(TEST_STREAM_KEY), any(Recruitment.class));
	}

	@Test
	@DisplayName("processETL() - Redis가 혼잡하지 않으면, 성공적으로 발행한다.")
	void processETL_Success_WhenRedisNotInCongestion() {
		// given
		setupDefaultMocks();
		doReturn(false).when(redisCongestionManager).isCongested(TEST_STREAM_KEY, TEST_GROUP_NAME);

		// when
		crawlerService.processETL(wait);

		// then
		verify(redisCongestionManager, times(1)).isCongested(TEST_STREAM_KEY, TEST_GROUP_NAME);
		verify(redisStreamProducer, times(1)).produceRecruitment(eq(TEST_STREAM_KEY), any(Recruitment.class));
	}

	private void setupDefaultMocks() {
		when(crawlerValidator.isValidDetailUrl(anyString())).thenReturn(true);
		when(crawlerValidator.isSkipTitle(anyString())).thenReturn(false);
		when(crawlerDataExtractor.extractCurrentUrl(any())).thenReturn("https://valid.url");
		when(crawlerDataExtractor.extractTitle(any())).thenReturn("Valid Title");
		when(crawlerDataExtractor.extractPostId(any())).thenReturn("123");
		when(crawlerDataExtractor.extractCompanyName(any())).thenReturn("Navik Corp.");

		LLMResponseDTO.Recruitment llmResponse = createLLMResponse();
		when(llmClient.getRecruitment(anyString())).thenReturn(llmResponse);

		float[] mockEmbedding = {0.1f, 0.2f, 0.3f};
		when(embeddingClient.embed(anyString())).thenReturn(mockEmbedding);
	}

	private LLMResponseDTO.Recruitment createLLMResponse() {
		LLMResponseDTO.Recruitment.Position position = LLMResponseDTO.Recruitment.Position.builder()
			.name("Backend Engineer")
			.jobType(JobType.BACKEND)
			.employmentType(EmploymentType.FULL_TIME)
			.experienceType(ExperienceType.ENTRY)
			.educationLevel(EducationLevel.BACHELOR)
			.areaType(AreaType.SEOUL)
			.detailAddress("서울시 강남구")
			.majorType(MajorType.ENGINEERING)
			.kpis(List.of("Spring Framework 경험", "JPA 사용 능력"))
			.build();

		return LLMResponseDTO.Recruitment.builder()
			.link("https://valid.url")
			.title("Valid Title")
			.postId("123")
			.companyName("Navik Corp.")
			.companyLogo("https://logo.url")
			.companySize(CompanySize.LARGE)
			.industryType(IndustryType.IT_TELECOMMUNICATION)
			.startDate(LocalDateTime.now())
			.endDate(LocalDateTime.now().plusDays(30))
			.positions(List.of(position))
			.summary("This is a summary of the recruitment.")
			.build();
	}
}