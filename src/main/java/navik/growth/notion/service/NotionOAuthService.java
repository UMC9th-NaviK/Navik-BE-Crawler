package navik.growth.notion.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import navik.growth.notion.dto.NotionWorkspaceToken;
import navik.growth.notion.exception.NotionNotConnectedException;
import navik.growth.notion.repository.NotionTokenRepository;

@Slf4j
@Service
public class NotionOAuthService {

	private final NotionTokenRepository tokenRepository;

	public NotionOAuthService(NotionTokenRepository tokenRepository) {
		this.tokenRepository = tokenRepository;
	}

	/**
	 * 사용자의 모든 워크스페이스 토큰 조회
	 */
	public List<NotionWorkspaceToken> getAllWorkspaceTokens(Long userId) {
		List<NotionWorkspaceToken> tokens = tokenRepository.findAllByUserId(userId);
		if (tokens.isEmpty()) {
			throw new NotionNotConnectedException(
					"Notion이 연동되지 않았습니다. 먼저 /api/notion/oauth/authorize?user_id=" + userId + " 로 연동해주세요.");
		}
		return tokens;
	}
}
