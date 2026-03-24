package navik.redis.congestion;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCongestionManager {

	private final RedisTemplate<String, String> redisTemplate;

	@Value("${spring.data.redis.congestion.max-memory-usage:0.7}")
	private double maxMemoryUsage;
	@Value("${spring.data.redis.congestion.max-stream-length:30}")
	private long maxStreamLength;
	@Value("${spring.data.redis.congestion.max-pending-threshold:10}")
	private long maxPendingThreshold;

	public boolean isCongested(String streamKey, String groupName) {
		return isMemoryUsageExceeded()
			|| isStreamLengthExceeded(streamKey)
			|| isPendingMessagesExceeded(streamKey, groupName);
	}

	/**
	 * Redis 메모리 사용량 임계치 검사
	 */
	public boolean isMemoryUsageExceeded() {
		Properties info = redisTemplate.execute((RedisCallback<Properties>)connection ->
			connection.info("memory")
		);

		if (info == null) {
			log.warn("Redis 메모리 정보를 가져올 수 없습니다.");
			return true;
		}

		long usedMemory = Long.parseLong(info.getProperty("used_memory"));
		long maxMemory = Long.parseLong(info.getProperty("maxmemory"));
		if (maxMemory == 0) {
			log.warn("Redis maxmemory가 0으로 설정되어 있습니다. OOM에 주의하세요. total System Memory로 계산합니다.");
			maxMemory = Long.parseLong(info.getProperty("total_system_memory"));
		}

		double ratio = (double)usedMemory / maxMemory;
		if (ratio > maxMemoryUsage) {
			log.warn("Redis 메모리 사용량이 임계치({}%)를 초과했습니다. 현재: {}%",
				maxMemoryUsage * 100, ratio * 100);
			return true;
		}

		return false;
	}

	/**
	 * 스트림의 길이 임계치 검사
	 */
	public boolean isStreamLengthExceeded(String streamKey) {
		Long streamLength = redisTemplate.opsForStream().size(streamKey);

		if (streamLength == null) {
			return false;
		}

		if (streamLength > maxStreamLength) {
			log.warn("스트림 [{}]의 길이가 임계치({}개)를 초과했습니다. 현재: {}개",
				streamKey, maxStreamLength, streamLength);
			return true;
		}

		return false;
	}

	/**
	 * Pending 메시지 개수 임계치 검사
	 */
	public boolean isPendingMessagesExceeded(String streamKey, String groupName) {
		PendingMessagesSummary pendingSummary = redisTemplate.opsForStream().pending(streamKey, groupName);

		if (pendingSummary == null) {
			return false;
		}

		long totalPendingMessages = pendingSummary.getTotalPendingMessages();
		if (totalPendingMessages > maxPendingThreshold) {
			log.warn("컨슈머 그룹 [{}]의 Pending 메시지 수가 임계치({}개)를 초과했습니다. 현재: {}개",
				groupName, maxPendingThreshold, totalPendingMessages);
			return true;
		}

		return false;
	}
}