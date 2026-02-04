package navik.crawler.scheduler;

import java.util.concurrent.Future;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.crawler.service.CrawlerService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

	private final ThreadPoolTaskScheduler scheduler;
	private final CrawlerService crawlerService;

	private volatile Future<?> currentTask;    // 스레드풀에서 비동기로 실행

	/**
	 * 매일 6시간마다 채용 공고를 추출합니다.
	 * 시각: 00:00, 06:00, 12:00, 18:00
	 */
	@Scheduled(cron = "0 0 */6 * * *")
	public boolean scheduledCrawl() {
		if (currentTask != null && !currentTask.isDone()) {
			log.info("이미 스케쥴링 중입니다.");
			return false;
		}
		currentTask = scheduler.submit(() -> crawlerService.scheduledCrawl(1));  // 각 직무 별로 50개 씩만
		return true;
	}

	public boolean stop() {
		if (currentTask != null && !currentTask.isDone()) {
			log.warn("스케쥴링 캔슬 요청");
			return currentTask.cancel(true);
		}
		return false;
	}

	public boolean isRunning() {
		return currentTask != null && !currentTask.isDone();
	}
}
