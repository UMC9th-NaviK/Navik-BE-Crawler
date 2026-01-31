package navik.growth.notion.repository;

import java.util.Optional;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Hash를 사용한 Notion OAuth 토큰 저장소
 * Key: notion:tokens
 * Field: userId
 * Value: accessToken
 */
@Slf4j
@Repository
public class NotionTokenRepository {

	private static final String HASH_KEY = "notion:tokens";

	private final HashOperations<String, String, String> hashOperations;

	public NotionTokenRepository(StringRedisTemplate redisTemplate) {
		this.hashOperations = redisTemplate.opsForHash();
	}

	/**
	 * 사용자의 Notion Access Token 저장
	 */
	public void save(String userId, String accessToken) {
		hashOperations.put(HASH_KEY, userId, accessToken);
		log.info("Notion 토큰 저장 완료: userId={}", userId);
	}

	/**
	 * 사용자의 Notion Access Token 조회
	 */
	public Optional<String> findByUserId(String userId) {
		String token = hashOperations.get(HASH_KEY, userId);
		return Optional.ofNullable(token);
	}

	/**
	 * 사용자의 Notion Access Token 삭제
	 */
	public void deleteByUserId(String userId) {
		hashOperations.delete(HASH_KEY, userId);
		log.info("Notion 토큰 삭제 완료: userId={}", userId);
	}

	/**
	 * 사용자의 Notion 연동 여부 확인
	 */
	public boolean existsByUserId(String userId) {
		return hashOperations.hasKey(HASH_KEY, userId);
	}
}
