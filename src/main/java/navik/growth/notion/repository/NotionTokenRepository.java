package navik.growth.notion.repository;

import java.util.List;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import navik.growth.notion.dto.NotionWorkspaceToken;

/**
 * Redis Hash를 사용한 Notion OAuth 토큰 저장소 (다중 워크스페이스 지원)
 * Key: notion:tokens:{userId}
 * Field: workspaceId
 * Value: JSON(NotionWorkspaceToken)
 */
@Slf4j
@Repository
public class NotionTokenRepository {

	private static final String KEY_PREFIX = "notion:tokens:";

	private final HashOperations<String, String, String> hashOperations;
	private final ObjectMapper objectMapper;

	public NotionTokenRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.hashOperations = redisTemplate.opsForHash();
		this.objectMapper = objectMapper;
	}

	/**
	 * 사용자의 모든 워크스페이스 토큰 조회
	 */
	public List<NotionWorkspaceToken> findAllByUserId(Long userId) {
		return hashOperations.entries(buildKey(userId)).values().stream()
				.map(this::deserialize)
				.toList();
	}

	private String buildKey(Long userId) {
		return KEY_PREFIX + userId;
	}

	private NotionWorkspaceToken deserialize(String json) {
		try {
			return objectMapper.readValue(json, NotionWorkspaceToken.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Notion 토큰 역직렬화 실패", e);
		}
	}
}
