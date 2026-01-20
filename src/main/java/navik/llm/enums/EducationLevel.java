package navik.llm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EducationLevel {

	HIGH_SCHOOL("고등학교 졸업", 1),
	ASSOCIATE("전문대 졸업", 2),
	BACHELOR("4년제 대학 졸업", 3),
	MASTER("석사 졸업", 4),
	DOCTOR("박사 졸업", 5);

	private final String label;
	private final int order;
}