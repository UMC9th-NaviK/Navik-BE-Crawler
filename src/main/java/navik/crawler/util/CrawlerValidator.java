package navik.crawler.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import navik.domain.crawler.constants.CrawlerConstant;
import navik.domain.crawler.constants.JobKoreaConstant;

@Component
public class CrawlerValidator {

	public boolean isSkipTitle(String title) {
		return CrawlerConstant.INVALID_RECRUITMENT_TITLES.stream()
			.anyMatch(title::contains);
	}

	public boolean isValidDetailUrl(String url) {
		Pattern pattern = Pattern.compile(JobKoreaConstant.RECRUITMENT_DETAIL_URL_PATTERN);
		Matcher matcher = pattern.matcher(url);
		return matcher.find();
	}
}
