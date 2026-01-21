package navik.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MajorType {

	IT("IT"),
	DESIGN_MEDIA("디자인/미디어"),
	NATURAL_SCIENCE_BIO("자연/과학/바이오"),
	ENGINEERING("공학"),
	HUMANITIES_SOCIAL_EDUCATION("인문/사회/교육"),
	BUSINESS_ECONOMY_OFFICE("경영/경제/사무");

	private final String label;
}
