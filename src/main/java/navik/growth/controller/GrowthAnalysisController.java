package navik.growth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.growth.dto.GrowthAnalysisRequest;
import navik.growth.dto.GrowthAnalysisResponse;
import navik.growth.service.GrowthAnalysisService;

/**
 * 성장 기록 분석 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/growth")
@RequiredArgsConstructor
public class GrowthAnalysisController {

	private final GrowthAnalysisService analysisService;

	/**
	 * 성장 기록 분석 API
	 *
	 * @param request 분석 요청 (sourceUrl, jobContext)
	 * @return 분석 결과 (title, summary, feedback, score)
	 */
	@PostMapping(value = "/analyze",
		consumes = "application/json; charset=UTF-8",
		produces = "application/json; charset=UTF-8")
	public ResponseEntity<GrowthAnalysisResponse> analyzeGrowthLog(
		@RequestBody @Valid GrowthAnalysisRequest request
	) {
		log.info("성장 기록 분석 요청 - userId: {}, URL: {}", request.userId(), request.sourceUrl());

		GrowthAnalysisResponse response = analysisService.analyze(request);

		return ResponseEntity.ok(response);
	}
}
