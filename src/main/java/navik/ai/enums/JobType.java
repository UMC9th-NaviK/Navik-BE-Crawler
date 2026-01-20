package navik.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobType {

	PM("프로덕트 매니저"),
	DESIGNER("프로덕트 디자이너"),
	FRONTEND("프론트엔드 개발자"),
	BACKEND("백엔드 개발자");

	private final String label;
}