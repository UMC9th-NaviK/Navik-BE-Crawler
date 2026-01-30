package navik.crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

	@Bean
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("crawler-scheduler-");
		scheduler.setRemoveOnCancelPolicy(true);
		scheduler.initialize();

		return scheduler;
	}
}
