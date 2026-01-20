package navik.llm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExperienceType {

	ENTRY("신입", 1),
	EXPERIENCED("경력", 2);

	private final String label;
	private final int order;
}
