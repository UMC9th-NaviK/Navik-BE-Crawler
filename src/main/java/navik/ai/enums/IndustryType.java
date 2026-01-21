package navik.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IndustryType {

	SERVICE("서비스업"),
	FINANCE_BANKING("금융·은행업"),
	IT_TELECOMMUNICATION("IT·정보통신업"),
	SALES_DISTRIBUTION("판매·유통업"),
	MANUFACTURING_CHEMICAL("제조·생산·화학업"),
	EDUCATION("교육업"),
	CONSTRUCTION("건설업"),
	MEDICAL_PHARMACEUTICAL("의료·제약업"),
	MEDIA_ADVERTISING("미디어·광고업"),
	CULTURE_ART_DESIGN("문화·예술·디자인업"),
	PUBLIC_ORGANIZATION("공공기관·협회");

	private final String label;
}
