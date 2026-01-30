package navik.crawler.scheduler;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.crawler.service.CrawlerService;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("prod")
public class CrawlerRecruitmentScheduler {

	private final CrawlerService crawlerService;

	/**
	 * 매일 6시간마다 채용 공고를 추출합니다.
	 * 시각: 00:00, 06:00, 12:00, 18:00
	 */
	@Scheduled(cron = "0 0 */6 * * *")
	public void scheduledCrawl() {
		log.info("스케쥴링이 시작되었습니다. 시간: {}", LocalDateTime.now());
		crawlerService.scheduledCrawl(1);  // 각 직무 별로 50개 씩만
		log.info("스케쥴링이 종료되었습니다. 시간: {}", LocalDateTime.now());
	}
}
