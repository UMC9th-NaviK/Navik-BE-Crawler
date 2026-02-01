package navik.ocr.dto;

import lombok.Builder;
import lombok.Getter;

public class OCRResponseDTO {

	@Getter
	@Builder
	public static class PdfOCR {
		private String text;
	}
}
