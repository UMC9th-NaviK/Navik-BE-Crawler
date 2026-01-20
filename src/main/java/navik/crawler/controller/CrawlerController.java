package navik.crawler.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.domain.crawler.service.CrawlerService;

/**
 * TODO: 실 환경에서 직접 트리거 해서 메모리 확인해보고, 서버 분리 결정할 예정입니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/crawler")
@RequiredArgsConstructor
public class CrawlerController implements CrawlerControllerDocs {

	private final CrawlerService crawlerService;

	/**
	 * TODO: 실 배포환경에서 직접 테스트 후 스케쥴러로 등록
	 */
	@PostMapping("/schedule")
	public void triggerSchedule(@RequestParam Integer pagesToCrawl) {
		crawlerService.scheduledCrawl(pagesToCrawl);
	}

	/**
	 * TODO: 개발자 따로, PM, 디자이너 따로
	 */
}
