package navik.crawler.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.crawler.constants.JobKoreaConstant;
import navik.crawler.factory.JsoupFactory;
import navik.ocr.client.OCRClient;

/**
 * 채용 공고 상세 페이지의 데이터 추출을 담당하는 메서드입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerDataExtractor {

	/**
	 * 구글 결제 문제로 인해 네이버 OCR 사용임시 대체,
	 */
	private final OCRClient ocrClient;
	private final JsoupFactory jsoupFactory;

	/**
	 * 현재 화면의 URL를 추출합니다.
	 */
	public String extractCurrentUrl(WebDriverWait wait) {
		return wait.until(WebDriver::getCurrentUrl);
	}

	/**
	 * 채용 공고의 고유 식별 번호를 추출합니다.
	 */
	public String extractPostId(WebDriverWait wait) {
		String url = extractCurrentUrl(wait);
		String recruitmentDetailUrlPattern = JobKoreaConstant.RECRUITMENT_DETAIL_URL_PATTERN;
		Matcher matcher = Pattern.compile(recruitmentDetailUrlPattern).matcher(url);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	/**
	 * 채용 공고의 제목을 추출합니다.
	 */
	public String extractTitle(WebDriverWait wait) {
		WebElement titleElement = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector("h1[data-sentry-element='Typography']")));
		return titleElement.getText();
	}

	/**
	 * 채용 공고의 회사명을 추출합니다.
	 */
	public String extractCompanyName(WebDriverWait wait) {
		WebElement companyNameElement = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector("h2[data-sentry-element='Typography']")));
		return companyNameElement.getText();
	}

	/**
	 * 채용 공고의 회사 로고를 추출합니다.
	 * '회사 내용 상세보기' 정적 페이지를 얻어 Jsoup으로 추출합니다.
	 */
	public String extractCompanyLogo(WebDriverWait wait) {
		WebElement companyMoreElement = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector("a[data-sentry-component='MoreButton']")));
		String companyUrl = companyMoreElement.getAttribute("href");
		Document companyDocument = jsoupFactory.createDocument(companyUrl);
		String companyLogo = companyDocument.selectFirst(".logo img").attr("src");
		return applyHttpsPrefix(companyLogo);
	}

	/**
	 * 채용 공고의 자격 요건을 추출합니다.
	 * 예시)
	 * 		경력: 2년이상
	 * 		학력: 무관
	 */
	public String extractQualification(WebDriverWait wait) {

		WebElement qualificationElement = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector("div[data-sentry-component='Qualification']")
		));

		List<WebElement> items = qualificationElement.findElements(
			By.cssSelector("div[data-sentry-component='QualificationItem']")
		);

		// 경력, 학력 2가지만 추출
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < Math.min(items.size(), 2); i++) {
			WebElement item = items.get(i);
			WebElement keyElement = item.findElement(By.cssSelector("span[style*='min-width:80px']"));

			String key = keyElement.getText();
			String value = item.getText().replace(key, "").trim();

			result.append(key).append(": ").append(value).append("\n");
		}

		return result.toString();
	}

	/**
	 * 채용 공고의 회사 정보를 추출합니다.
	 * 예시)
	 * 		사원수: 50명 이하
	 * 		기업구분: 중소기업 (비상장)
	 * 		산업(업종): 전자상거래 소매 중개업
	 * 		위치: 서울 강남구 논현로 ...
	 */
	public String extractCompanyInfo(WebDriverWait wait) {

		WebElement companyInfoElement = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector("div[data-sentry-component='CorpInformation']")
		));

		List<WebElement> items = companyInfoElement.findElements(
			By.cssSelector("div[data-sentry-component='CorpInformationBox']")
		);

		// 사원수, 기업구분, 산업(업종), 위치 등 추출
		StringBuilder result = new StringBuilder();

		for (WebElement item : items) {
			WebElement keyElement = item.findElement(By.cssSelector("span[class*='Typography_variant_size13']"));
			WebElement valueElement = item.findElement(By.cssSelector("div[class*='Typography_variant_size14']"));

			String key = keyElement.getText().trim();
			String value = valueElement.getText().trim();

			result.append(key).append(": ").append(value).append("\n");
		}

		return result.toString();
	}

	/**
	 * 채용 공고의 시작 시간과 마감 시간을 추출합니다.
	 * 예시)
	 * 		남은기간 26일 04:01:43, 시작일 2026.01.05(월) 마감일 2026.02.04(수)
	 */
	public String extractTimeInfo(WebDriverWait wait) {
		String countdownText = "알수없음";
		try {
			WebElement countdownElement = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.cssSelector("span[data-sentry-component='Countdown']")
			));
			countdownText = countdownElement.getText();
		} catch (Exception exception) {
			log.info("남은 기간이 표시되지 않은 공고입니다.");
		}
		WebElement dateElement = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector("div[data-sentry-component='SimpleTable']")
		));
		return "남은기간 " + countdownText + ", " + dateElement.getText();
	}

	/**
	 * 채용 공고의 모집 요강을 추출합니다.
	 */
	public String extractOutline(WebDriverWait wait) {
		WebElement outlineElement = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector(
				"div[data-sentry-element='Flex'][data-sentry-source-file='index.tsx'].Flex_display_flex__i0l0hl2.Flex_gap_space28__i0l0hl2a.Flex_direction_column__i0l0hl4")
		));
		return outlineElement.getText();
	}

	/**
	 * iframe으로부터 채용 공고 정적 페이지를 얻고, Jsoup 기반으로 상세 내용을 파싱합니다.
	 *
	 * 잡코리아 채용 공고의 유형은 다음과 같습니다.
	 * 	 1. HTML 테이블 태그 기반
	 * 	 2. 이미지 기반
	 * 위 두 가지 경우의 수를 모두 처리합니다.
	 */
	public String extractRecruitmentDetail(WebDriverWait wait) {
		String recruitmentDetailUrl = JobKoreaConstant.BASE_URL + extractIframeUrl(wait);
		Document recruitmentDocument = jsoupFactory.createDocument(recruitmentDetailUrl);
		return extractRecruitmentTable(recruitmentDocument)    // HTML Table 추출
			+ "<OCR 결과>"
			+ extractRecruitmentImage(recruitmentDocument)     // 이미지 추출
			+ "</OCR 결과>";
	}

	/**
	 * iframe으로부터 정적 공고 페이지의 url를 추출합니다.
	 */
	private String extractIframeUrl(WebDriverWait wait) {
		WebElement detailIframe = wait.until(ExpectedConditions.presenceOfElementLocated(
			By.cssSelector("#details-section iframe")
		));
		return detailIframe.getDomAttribute("src");
	}

	/**
	 * 공고 페이지의 모집 정보를 HTML Table 태그로 추출합니다.
	 */
	private String extractRecruitmentTable(Document document) {

		Element tableElement = document.selectFirst("td.detailTable");
		if (tableElement == null) {
			return "";
		}

		// HTML 테이블 태그만 남기도록 Safelist 설정
		Safelist tableTags = Safelist.none()
			.addTags("table", "thead", "tbody", "tfoot")
			.addTags("tr", "th", "td")
			.addTags("caption", "colgroup", "col")
			.addAttributes("td", "rowspan", "colspan");

		StringBuilder result = new StringBuilder();
		result.append(Jsoup.clean(tableElement.html(), tableTags));

		return result.toString();
	}

	private String extractRecruitmentImage(Document document) {

		Elements images = document.select("td.detailTable img");

		// 1. 이미지 src 추출, 이미지 중복 제거 및 순서 유지를 위한 LinkedHashSet
		LinkedHashSet<String> imageSet = new LinkedHashSet<>();
		for (Element img : images) {
			if (img.closest("p.visual") != null) {
				continue;    // 채용 공고가 아닌, 회사 소개 이미지에 해당하는 경우 제외
			}
			String imgUrl = applyHttpsPrefix(img.absUrl("src"));
			imageSet.add(imgUrl);
		}

		// 2. 필터링된 이미지들에 대해 OCR 호출
		StringBuilder result = new StringBuilder();
		for (String imgUrl : imageSet) {
			String imageText = ocrClient.extractFromImageUrl(imgUrl);
			if (!imageText.isBlank()) {
				result.append(imageText).append("\n\n");
			}
		}

		// 3. 결과 반환
		return result.toString();
	}

	/**
	 * 이미지 url에 'https:'가 누락된 경우, 추가합니다.
	 */
	private String applyHttpsPrefix(String url) {
		if (url.startsWith("//"))
			return "https:" + url;
		return url;
	}
}
