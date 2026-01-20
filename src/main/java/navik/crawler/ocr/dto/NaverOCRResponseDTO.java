package navik.crawler.ocr.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class NaverOCRResponseDTO {

	@Getter
	@NoArgsConstructor
	public static class OcrResponse {
		private String version;
		private String requestId;
		private long timestamp;
		private List<Image> images;
	}

	@Getter
	@NoArgsConstructor
	public static class Image {
		private String uid;
		private String name;
		private String inferResult;
		private String message;
		private ValidationResult validationResult;
		private List<Field> fields;
	}

	@Getter
	@NoArgsConstructor
	public static class ValidationResult {
		private String result;
	}

	@Getter
	@NoArgsConstructor
	public static class Field {
		private String valueType;
		private BoundingPoly boundingPoly;
		private String inferText;    // 인식된 텍스트
		private double inferConfidence;
		private String type;
		private boolean lineBreak;
	}

	@Getter
	@NoArgsConstructor
	public static class BoundingPoly {
		private List<Vertex> vertices;
	}

	@Getter
	@NoArgsConstructor
	public static class Vertex {
		private double x;
		private double y;
	}
}
