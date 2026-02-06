package navik.growth.notion.repository;

import java.util.List;
import java.util.Optional;

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
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public NotionTokenRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.hashOperations = redisTemplate.opsForHash();
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * 워크스페이스 토큰 저장 (같은 workspaceId면 자동 덮어쓰기)
	 */
	public void save(String userId, NotionWorkspaceToken token) {
		try {
			String json = objectMapper.writeValueAsString(token);
			hashOperations.put(buildKey(userId), token.workspaceId(), json);
			log.info("Notion 토큰 저장 완료: userId={}, workspaceId={}", userId, token.workspaceId());
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Notion 토큰 직렬화 실패", e);
		}
	}

	/**
	 * 사용자의 모든 워크스페이스 토큰 조회
	 */
	public List<NotionWorkspaceToken> findAllByUserId(String userId) {
		return hashOperations.entries(buildKey(userId)).values().stream()
			.map(this::deserialize)
			.toList();
	}

	/**
	 * 특정 워크스페이스 토큰 조회
	 */
	public Optional<NotionWorkspaceToken> findByUserIdAndWorkspaceId(String userId, String workspaceId) {
		String json = hashOperations.get(buildKey(userId), workspaceId);
		return Optional.ofNullable(json).map(this::deserialize);
	}

	/**
	 * 특정 워크스페이스 토큰 삭제
	 */
	public void deleteByUserIdAndWorkspaceId(String userId, String workspaceId) {
		hashOperations.delete(buildKey(userId), workspaceId);
		log.info("Notion 토큰 삭제 완료: userId={}, workspaceId={}", userId, workspaceId);
	}

	/**
	 * 사용자의 모든 워크스페이스 토큰 삭제
	 */
	public void deleteAllByUserId(String userId) {
		redisTemplate.delete(buildKey(userId));
		log.info("Notion 전체 토큰 삭제 완료: userId={}", userId);
	}

	/**
	 * 사용자의 워크스페이스 연동 여부 확인 (1개 이상 연동)
	 */
	public boolean existsByUserId(String userId) {
		Long size = hashOperations.size(buildKey(userId));
		return size != null && size > 0;
	}

	private String buildKey(String userId) {
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
