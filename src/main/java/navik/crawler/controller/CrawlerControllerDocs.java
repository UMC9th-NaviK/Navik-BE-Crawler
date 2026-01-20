package navik.crawler.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(name = "Crawl", description = "채용 공고 크롤링 관련 API")
public interface CrawlerControllerDocs {

	@Operation(summary = "스케쥴러 수동 트리거", description = """
		'웹개발자', 'PM', '디자이너' 순으로 채용 공고 크롤링을 진행합니다.
		<br>위 3개의 직무 별로 'pagesToCrawl * 50건'의 채용 공고를 처리합니다.
		<br>따라서 총 처리되는 개수는 '3 * pagesToCrawl * 50' 입니다.
		<br>크롤링할 페이지 수는 1~5 사이 값만 허용됩니다.
		""")
	void triggerSchedule(@Parameter(description = "크롤링할 페이지 수") @Min(1) @Max(5) Integer pagesToCrawl);
}
