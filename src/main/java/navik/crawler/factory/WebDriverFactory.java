package navik.crawler.factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import navik.crawler.constants.CrawlerConstant;

@Component
public class WebDriverFactory {

	public WebDriver createChromeDriver() {

		ChromeOptions options = new ChromeOptions();
		options.addArguments("--start-maximized");
		options.addArguments("--disable-popup-blocking");
		options.addArguments("--disable-gpu");
		options.addArguments("--window-size=1920,1080");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--remote-allow-origins=*");
		options.addArguments("--disable-blink-features=AutomationControlled");
		options.addArguments("--headless=new"); // UI 없이 실행

		// 멀티스레딩 시 포트 충돌 처리 필요

		return new ChromeDriver(options);
	}

	public WebDriverWait createDriverWait(WebDriver driver) {
		return new WebDriverWait(driver, CrawlerConstant.CLICK_WAIT_TIME);
	}
}
