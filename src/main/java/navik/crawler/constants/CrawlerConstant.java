package navik.crawler.constants;

import java.time.Duration;
import java.util.List;

public class CrawlerConstant {
	public static final Duration CLICK_WAIT_TIME = Duration.ofSeconds(5); // selenium 대기 시간
	public static final int CRAWL_PAGES_PER_JOB = 2; // 2개 페이지 파싱
	public static final List<String> INVALID_RECRUITMENT_TITLES = List.of("교육", "국비", "캠프", "취업");
}
