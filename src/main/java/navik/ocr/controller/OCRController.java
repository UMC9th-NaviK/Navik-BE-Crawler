package navik.ocr.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import navik.ocr.client.OCRClient;
import navik.ocr.dto.OCRRequestDTO;
import navik.ocr.dto.OCRResponseDTO;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ocr")
public class OCRController {

	private final OCRClient ocrClient;

	@PostMapping("/pdf")
	public ResponseEntity<OCRResponseDTO.PdfOCR> extractTextFromPdf(
		@RequestBody OCRRequestDTO.PdfOCR request) {

		String text = ocrClient.extractFromPdfUrl(request.getPdfUrl());
		System.out.println(text);

		return ResponseEntity.ok(
			OCRResponseDTO.PdfOCR.builder()
				.text(text)
				.build()
		);
	}
}
