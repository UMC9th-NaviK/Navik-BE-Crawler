package navik.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanySize {

	LARGE("대기업"),
	MID_LARGE("중견기업"),
	SMALL("중소기업"),
	PUBLIC("공기업"),
	FOREIGN("외국계기업");

	private final String label;
}
