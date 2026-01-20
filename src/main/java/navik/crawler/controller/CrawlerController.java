package navik.crawler.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.domain.crawler.service.CrawlerService;

@Slf4j
@RestController
@RequestMapping("/v1/crawler")
@RequiredArgsConstructor
public class CrawlerController implements CrawlerControllerDocs {

	private final CrawlerService crawlerService;

	/**
	 * TODO: 스케쥴러로 등록
	 */
	@PostMapping("/schedule")
	public void triggerSchedule(@RequestParam Integer pagesToCrawl) {
		crawlerService.scheduledCrawl(pagesToCrawl);
	}
}
