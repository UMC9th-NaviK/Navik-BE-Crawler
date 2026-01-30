package navik.crawler.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import navik.crawler.service.CrawlerControlService;

@RestController
@RequiredArgsConstructor
public class CrawlerController {

	private final CrawlerControlService crawlerControlService;

	@GetMapping("/trigger")
	public ResponseEntity<String> trigger() {
		boolean success = crawlerControlService.triggerSchedule();

		if (success) {
			return ResponseEntity.ok("스케쥴링 트리거 성공");
		}
		return ResponseEntity.ok("이미 처리 중인 스케쥴링입니다.");
	}

	@GetMapping("/stop")
	public ResponseEntity<String> stop() {
		boolean cancelled = crawlerControlService.stopCrawler();

		if (cancelled) {
			return ResponseEntity.ok("스케쥴링 강제 종료 완료");
		}
		return ResponseEntity.ok("실행 중인 스케쥴링이 없습니다.");
	}

	@GetMapping("/status")
	public ResponseEntity<String> status() {
		return ResponseEntity.ok(
			Map.of(
				"running", crawlerControlService.isRunning(),
				"timestamp", LocalDateTime.now()
			).toString()
		);
	}
}
