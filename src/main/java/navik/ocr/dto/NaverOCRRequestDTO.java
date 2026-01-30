package navik.ocr.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

public class NaverOCRRequestDTO {

	@Getter
	@Builder
	public static class Request {
		private String version;   // V2 엔진 권장
		private String requestId;    // 요청 식별을 위한 UUID
		private long timestamp;    // 요청 시각
		private String lang;    // 이미지에 적힌 언어
		private List<Image> images;    // 호출 당 1개의 이미지 OCR
	}

	@Getter
	@Builder
	public static class Image {
		private String format;    // jpg, jpeg, png, pdf, tif, tiff
		private String name;    // 이미지 식별 이름
		private String url;    // 공개된 이미지 URL 넘기기
		private String data;   // 또는 Base64 인코딩 데이터 넘기기 (url과 충돌 시 우선 적용)
	}
}
