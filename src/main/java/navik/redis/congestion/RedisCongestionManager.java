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

	@Value("${spring.data.redis.congestion.max-memory-usage}:0.7")
	private double maxMemoryUsage;
	@Value("${spring.data.redis.congestion.max-stream-length:30}")
	private long maxStreamLength;
	@Value("${spring.data.redis.congestion.max-pending-threshold:10}")
	private long maxPendingThreshold;

	public boolean isCongested(String streamKey, String groupName) {

		// 1. 메모리 사용량으로 혼잡 판단 (> 70%)
		Properties info = redisTemplate.execute((RedisCallback<Properties>)connection ->
			connection.info("memory")
		);
		long usedMemory = Long.parseLong(info.getProperty("used_memory"));
		long maxMemory = Long.parseLong(info.getProperty("maxmemory")); // default : 3GB(32bit), 0(64bit)
		if (maxMemory == 0) {
			log.warn("Redis maxmemory가 0으로 설정되어 있습니다. OOM 주의");
			maxMemory = Long.parseLong(info.getProperty("total_system_memory"));
		}
		if ((double)(usedMemory / maxMemory) > maxMemoryUsage)
			return true;

		// 2. 스트림 길이로 혼잡 판단
		Long streamLength = redisTemplate.opsForStream().size(streamKey);
		if (streamLength != null && streamLength > maxStreamLength)
			return true;

		// 3. Pending 개수로 혼잡 판단
		PendingMessagesSummary pendingSummary = redisTemplate.opsForStream().pending(streamKey, groupName);
		if (pendingSummary != null && pendingSummary.getTotalPendingMessages() > maxPendingThreshold) {
			log.warn("Consumer Group [{}]의 Pending 채용 공고가 임계치를 초과하였습니다 : {}", groupName,
				pendingSummary.getTotalPendingMessages());
			return true;
		}

		return false;
	}
}