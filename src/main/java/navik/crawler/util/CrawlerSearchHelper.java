package navik.crawler.util;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import navik.crawler.enums.JobCode;

/**
 * 크롤러의 검색 작업을 담당하는 클래스입니다.
 */
@Component
public class CrawlerSearchHelper {

	/**
	 * 검색 전 직무 필터를 적용하는 메서드입니다.
	 */
	public void applyJobFilter(WebDriverWait wait, JobCode jobCode) {
		String step1 = jobCode.getJobCode();
		String step2 = jobCode.getDetailCode();

		WebElement step1Filter = wait.until(
			ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for='duty_step1_" + step1 + "']")));
		wait.until(ExpectedConditions.elementToBeClickable(step1Filter)).click();

		WebElement step2Filter = wait.until(
			ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for='duty_step2_" + step2 + "']")));
		wait.until(ExpectedConditions.elementToBeClickable(step2Filter)).click();
	}

	/**
	 * 필터 적용 이후 검색을 실행하는 메서드입니다.
	 */
	public void search(WebDriverWait wait) {
		WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("dev-btn-search")));
		searchButton.click();
		try {
			Thread.sleep(5000); // 검색 결과 대기를 위한 sleep
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 검색 이후 '최신 업데이트 순'으로 정렬하는 메서드입니다.
	 */
	public void applySort(WebDriverWait wait) {
		WebElement sortElement = wait.until(ExpectedConditions.elementToBeClickable(
			By.cssSelector("#orderTab option[value='3']")    // 3: 최신 업데이트 순
		));
		sortElement.click();
	}

	/**
	 * 검색 이후 '한 페이지에 보이는 공고 개수'를 설정하는 메서드입니다.
	 */
	public void applyQuantity(WebDriverWait wait) {
		WebElement quantityElement = wait.until(ExpectedConditions.elementToBeClickable(
			By.cssSelector("#pstab option[value='50']")    // 한 페이지 당 50개씩 노출
		));
		quantityElement.click();
	}
}
