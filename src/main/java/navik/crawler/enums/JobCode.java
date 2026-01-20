package navik.crawler.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobCode {

	DEVELOPER("10031", "1000231"),
	PRODUCT_MANAGER("10026", "1000185"),
	DESIGNER("10032", "1000248");

	private final String jobCode;
	private final String detailCode;
}
