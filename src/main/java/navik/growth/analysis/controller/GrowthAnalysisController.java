package navik.growth.analysis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.growth.analysis.dto.AnalysisRequest;
import navik.growth.analysis.dto.AnalysisResponse;
import navik.growth.analysis.service.GrowthAnalysisService;

/**
 * 성장 기록 분석 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/v1/growth-logs/evaluate/user-input")
@RequiredArgsConstructor
public class GrowthAnalysisController {

	private final GrowthAnalysisService analysisService;

	/**
	 * 성장 기록 분석 API
	 *
	 * @param request 분석 요청 (userId, jobId, levelValue, context)
	 * @return 분석 결과 (title, content, 10개 kpiDeltas)
	 */
	@PostMapping(consumes = "application/json; charset=UTF-8", produces = "application/json; charset=UTF-8")
	public ResponseEntity<AnalysisResponse.GrowthAnalysisResponse> analyzeGrowthLog(
		@RequestBody @Valid AnalysisRequest.GrowthAnalysisRequest request) {
		log.info("성장 기록 분석 요청 - userId: {}, jobId: {}, level: {}", request.userId(), request.jobId(),
			request.levelValue());

		AnalysisResponse.GrowthAnalysisResponse response = analysisService.analyze(request);

		return ResponseEntity.ok(response);
	}
}
