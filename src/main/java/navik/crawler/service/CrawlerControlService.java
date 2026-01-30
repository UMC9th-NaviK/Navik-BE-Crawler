package navik.crawler.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import navik.crawler.scheduler.CrawlerScheduler;

@Service
@RequiredArgsConstructor
public class CrawlerControlService {

	private final CrawlerScheduler scheduler;

	public boolean triggerSchedule() {
		return scheduler.scheduledCrawl();
	}

	public boolean stopCrawler() {
		return scheduler.stop();
	}

	public boolean isRunning() {
		return scheduler.isRunning();
	}
}
