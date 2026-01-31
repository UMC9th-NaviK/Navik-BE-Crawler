package navik.growth.notion.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.growth.notion.dto.NotionOAuthResponse;
import navik.growth.notion.dto.NotionOAuthResponse.WorkspaceInfo;
import navik.growth.notion.service.NotionOAuthService;

@Slf4j
@RestController
@RequestMapping("/api/notion/oauth")
@RequiredArgsConstructor
public class NotionOAuthController {

	private final NotionOAuthService oAuthService;

	/**
	 * STEP 1: Notion OAuth 인증 시작
	 * 사용자를 Notion 인증 페이지로 리다이렉트
	 *
	 * @param userId 사용자 식별자
	 */
	@GetMapping("/authorize")
	public ResponseEntity<Void> authorize(@RequestParam("user_id") String userId) {
		log.info("Notion OAuth 인증 시작: userId={}", userId);

		String authorizationUrl = oAuthService.buildAuthorizationUrl(userId);

		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(authorizationUrl))
			.build();
	}

	/**
	 * STEP 4-5: Notion에서 콜백 수신 및 토큰 교환
	 * Authorization Code를 Access Token으로 교환 후 저장
	 *
	 * @param code  Authorization Code (Notion에서 전달)
	 * @param state userId (인증 시작 시 전달한 값)
	 * @param error 에러 코드 (사용자가 거부한 경우)
	 */
	@GetMapping("/callback")
	public ResponseEntity<NotionOAuthResponse.CallbackResponse> callback(
		@RequestParam(value = "code", required = false) String code,
		@RequestParam(value = "state", required = false) String state,
		@RequestParam(value = "error", required = false) String error
	) {
		// 사용자가 권한 부여를 거부한 경우
		if (error != null) {
			log.warn("Notion OAuth 거부됨: error={}, userId={}", error, state);
			return ResponseEntity.badRequest()
				.body(new NotionOAuthResponse.CallbackResponse(false, "Notion 연동이 거부되었습니다.", state, null, null));
		}

		// state(userId) 검증
		if (state == null || state.isBlank()) {
			log.error("Notion OAuth 콜백 오류: state 누락");
			return ResponseEntity.badRequest()
				.body(new NotionOAuthResponse.CallbackResponse(false, "잘못된 요청입니다. (state 누락)", null, null, null));
		}

		try {
			log.info("Notion OAuth 콜백 수신: userId={}", state);

			// Authorization Code → Access Token 교환
			NotionOAuthResponse.TokenResponse tokenResponse = oAuthService.exchangeCodeForToken(code);

			// 토큰 저장 (TokenResponse 전체 전달)
			oAuthService.saveToken(state, tokenResponse);

			log.info("Notion 연동 완료: userId={}, workspace={}, workspaceId={}",
				state, tokenResponse.workspaceName(), tokenResponse.workspaceId());

			return ResponseEntity.ok(new NotionOAuthResponse.CallbackResponse(
				true,
				"Notion 연동이 완료되었습니다!",
				state,
				tokenResponse.workspaceName(),
				tokenResponse.workspaceId()
			));

		} catch (Exception e) {
			log.error("Notion OAuth 처리 실패: userId={}", state, e);
			return ResponseEntity.internalServerError()
				.body(
					new NotionOAuthResponse.CallbackResponse(false, "Notion 연동 중 오류가 발생했습니다: " + e.getMessage(), state,
						null, null));
		}
	}

	/**
	 * 연동 상태 확인 (워크스페이스 목록 포함)
	 */
	@GetMapping("/status")
	public ResponseEntity<NotionOAuthResponse.StatusResponse> status(@RequestParam("user_id") String userId) {
		boolean connected = oAuthService.isConnected(userId);
		List<WorkspaceInfo> workspaces = oAuthService.getConnectedWorkspaces(userId);
		return ResponseEntity.ok(new NotionOAuthResponse.StatusResponse(userId, connected, workspaces));
	}

	/**
	 * 연동 해제 (workspace_id 지정 시 해당 워크스페이스만, 미지정 시 전체 해제)
	 */
	@DeleteMapping("/disconnect")
	public ResponseEntity<NotionOAuthResponse.DisconnectResponse> disconnect(
		@RequestParam("user_id") String userId,
		@RequestParam(value = "workspace_id", required = false) String workspaceId
	) {
		if (workspaceId != null && !workspaceId.isBlank()) {
			oAuthService.disconnect(userId, workspaceId);
			log.info("Notion 워크스페이스 연동 해제: userId={}, workspaceId={}", userId, workspaceId);
			return ResponseEntity.ok(new NotionOAuthResponse.DisconnectResponse(
				userId, "Notion 워크스페이스 연동이 해제되었습니다.", workspaceId));
		} else {
			oAuthService.disconnectAll(userId);
			log.info("Notion 전체 연동 해제: userId={}", userId);
			return ResponseEntity.ok(new NotionOAuthResponse.DisconnectResponse(
				userId, "Notion 전체 연동이 해제되었습니다.", null));
		}
	}
}
