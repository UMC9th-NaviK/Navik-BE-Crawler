package navik.llm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmploymentType {

	FULL_TIME("정규직"),
	CONTRACT("계약직"),
	INTERN("인턴"),
	FREELANCER("프리랜서");

	private final String label;
}
