package navik.ocr.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class OCRRequestDTO {

	@Getter
	@NoArgsConstructor
	public static class PdfOCR {
		private String pdfUrl;
	}
}
