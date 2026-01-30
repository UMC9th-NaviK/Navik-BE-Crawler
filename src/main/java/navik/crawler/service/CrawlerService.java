package navik.crawler.service;

import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.ai.client.EmbeddingClient;
import navik.ai.client.LLMClient;
import navik.ai.dto.LLMResponseDTO;
import navik.crawler.constants.JobKoreaConstant;
import navik.crawler.dto.CrawledRecruitment;
import navik.crawler.dto.Recruitment;
import navik.crawler.enums.JobCode;
import navik.crawler.factory.WebDriverFactory;
import navik.crawler.util.CrawlerDataExtractor;
import navik.crawler.util.CrawlerSearchHelper;
import navik.crawler.util.CrawlerValidator;
import navik.redis.service.RedisStreamProducer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

	private final WebDriverFactory webDriverFactory;
	private final CrawlerSearchHelper crawlerSearchHelper;
	private final CrawlerDataExtractor crawlerDataExtractor;
	private final CrawlerValidator crawlerValidator;
	private final LLMClient llmClient;
	private final EmbeddingClient embeddingClient;
	private final RedisStreamProducer redisStreamProducer;

	@Value("${spring.data.redis.stream.keys.crawl}")
	private String recruitmentStreamKey;

	/**
	 * 스케쥴링에 의해 주기적으로 실행되는 메서드입니다.
	 */
	public void scheduledCrawl(Integer pagesToCrawl) {
		// 1. 크롬 드라이버 생성
		WebDriver driver = webDriverFactory.createChromeDriver();
		WebDriverWait wait = webDriverFactory.createDriverWait(driver);

		// 2. JobCode(직무)별 크롤링
		try {
			for (JobCode jobCode : JobCode.values()) {
				log.info("=== [{}] 직무 크롤링 시작 ===", jobCode.name());
				driver.get(JobKoreaConstant.RECRUITMENT_LIST_URL);
				search(wait, jobCode);    // 직무 기반 필터 적용 및 검색
				processPages(driver, wait, pagesToCrawl); // 페이지 수 만큼 파싱
			}
		} catch (Exception exception) {
			log.error("스케쥴링 작업 중 오류 발생\n{}", exception.getMessage());
		} finally {
			driver.quit(); // 리소스 해제
		}
	}

	/**
	 * 필터를 적용하여 직무 별 검색을 수행합니다.
	 */
	private void search(WebDriverWait wait, JobCode jobCode) {
		crawlerSearchHelper.applyJobFilter(wait, jobCode);    // 필터 적용
		crawlerSearchHelper.search(wait);    // 검색
		crawlerSearchHelper.applySort(wait); // 정렬
		crawlerSearchHelper.applyQuantity(wait);    // 한 페이지 당 보여질 개수 설정
	}

	/**
	 * 검색 이후 pages만큼 페이지를 처리합니다.
	 */
	private void processPages(WebDriver driver, WebDriverWait wait, int pages) {

		// 1. base window 기억
		String baseUrl = driver.getCurrentUrl();
		String originalWindow = driver.getWindowHandle();
		String newWindow = "";

		// 2. 해당 페이지로 이동
		for (int currentPage = 1; currentPage <= pages; currentPage++) {

			// currentPage로 이동
			String newUrl = baseUrl.replace("#anchorGICnt_\\d+", "#anchorGICnt_" + currentPage);
			driver.get(newUrl);

			// 공고 목록 대기
			wait.until(ExpectedConditions.presenceOfElementLocated(
				By.cssSelector("tr[data-index='0']")
			));

			// 전체 공고 개수 확인
			List<WebElement> posts = driver.findElements(By.cssSelector("tr[data-index] strong a"));
			log.info("{}페이지에서 총 {}개의 공고 발견", currentPage, posts.size());

			// 공고 처리
			for (WebElement post : posts) {

				// 클릭 및 대기
				post.click();
				wait.until(ExpectedConditions.numberOfWindowsToBe(2));

				// 해당 공고 창 추출
				Optional<String> postWindow = driver.getWindowHandles().stream()
					.filter(handle -> !handle.equals(originalWindow))
					.findFirst();

				if (postWindow.isEmpty()) {
					log.error("새 창을 찾지 못하였습니다.");
					continue;
				}

				// 창 전환 및 추출
				try {
					driver.switchTo().window(newWindow);
					wait.until(ExpectedConditions.not(
						ExpectedConditions.urlToBe("about:blank")
					));
					processETL(wait);
				} catch (Exception exception) {
					log.error("상세 페이지 처리 중 오류 발생: {}", exception.getMessage());
				} finally {
					driver.close();
					driver.switchTo().window(originalWindow);
				}
			}
		}
	}

	/**
	 * 채용 공고에 대한 데이터 추출, 변환, 적재 작업을 수행하는 메서드입니다.
	 */
	private void processETL(WebDriverWait wait) {

		// 1. 채용 공고 상세 페이지 url 유효성 검사
		String link = crawlerDataExtractor.extractCurrentUrl(wait);
		if (!crawlerValidator.isValidDetailUrl(link)) {
			log.info("유효하지 않은 채용 공고 링크: {}", link);
			return;
		}

		// 2. 제목 유효성 검사
		String title = crawlerDataExtractor.extractTitle(wait);
		if (crawlerValidator.isSkipTitle(title)) {
			log.info("유효하지 않은 채용 공고 제목: {}", title);
			return;
		}

		// 3. 나머지 데이터 추출 및 DTO 작성
		CrawledRecruitment crawledRecruitment = CrawledRecruitment.builder()
			.link(link)
			.title(title)
			.postId(crawlerDataExtractor.extractPostId(wait))
			.companyName(crawlerDataExtractor.extractCompanyName(wait))
			.companyLogo(crawlerDataExtractor.extractCompanyLogo(wait))
			.companyInfo(crawlerDataExtractor.extractCompanyInfo(wait))
			.qualification(crawlerDataExtractor.extractQualification(wait))
			.timeInfo(crawlerDataExtractor.extractTimeInfo(wait))
			.outline(crawlerDataExtractor.extractOutline(wait))
			.recruitmentDetail(crawlerDataExtractor.extractRecruitmentDetail(wait))
			.build();

		// 4. LLM 호출
		String html = crawledRecruitment.toHtmlString();
		LLMResponseDTO.Recruitment llmResult = llmClient.getRecruitment(html);
		log.info("[LLM 채용 공고 결과] {}", llmResult);

		// 5. KPI 임베딩
		List<Recruitment.Position> positions = llmResult.getPositions().stream()
			.map(llmPosition -> {
				List<Recruitment.Position.KPI> kpis = llmPosition.getKpis().stream()
					.map(kpi -> {
						float[] embedding = embeddingClient.embed(kpi);
						return Recruitment.Position.KPI.builder()
							.kpi(kpi)
							.embedding(embedding)
							.build();
					}).toList();
				return Recruitment.Position.builder()
					.name(llmPosition.getName())
					.jobType(llmPosition.getJobType())
					.employmentType(llmPosition.getEmploymentType())
					.experienceType(llmPosition.getExperienceType())
					.educationLevel(llmPosition.getEducationLevel())
					.areaType(llmPosition.getAreaType())
					.detailAddress(llmPosition.getDetailAddress())
					.majorType(llmPosition.getMajorType())
					.kpis(kpis)
					.build();
			}).toList();

		// 6. DTO 생성
		Recruitment recruitment = Recruitment.builder()
			.link(llmResult.getLink())
			.title(llmResult.getTitle())
			.postId(llmResult.getPostId())
			.companyName(llmResult.getCompanyName())
			.companyLogo(llmResult.getCompanyLogo())
			.companySize(llmResult.getCompanySize())
			.industryType(llmResult.getIndustryType())
			.startDate(llmResult.getStartDate())
			.endDate(llmResult.getEndDate())
			.positions(positions)
			.summary(llmResult.getSummary())
			.build();

		// 7. 발행
		redisStreamProducer.produceRecruitment(recruitmentStreamKey, recruitment);
	}
}