package navik.redis.congestion;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import navik.redis.config.RedisTestContainersConfig;

@SpringBootTest
class RedisCongestionManagerTest extends RedisTestContainersConfig {

	@Autowired
	private RedisCongestionManager redisCongestionManager;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	private final String TEST_STREAM_KEY = "test-stream";
	private final String TEST_GROUP_NAME = "test-group";
	private final String TEST_CONSUMER_NAME = "test-consumer";

	@BeforeEach
	void setUp() {
		// 각 테스트마다 Flush
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

		ReflectionTestUtils.setField(redisCongestionManager, "maxMemoryUsage", 0.7);
		ReflectionTestUtils.setField(redisCongestionManager, "maxStreamLength", 30L);
		ReflectionTestUtils.setField(redisCongestionManager, "maxPendingThreshold", 10L);

		try {
			MapRecord<String, Object, Object> record = MapRecord.create(TEST_STREAM_KEY, Map.of("init", "true"));
			redisTemplate.opsForStream().add(record);
			redisTemplate.opsForStream().createGroup(TEST_STREAM_KEY, ReadOffset.from("0-0"), TEST_GROUP_NAME);
		} catch (Exception e) {
			// 이미 존재해도 무시
		}
	}

	@Test
	@DisplayName("isMemoryUsageExceeded() 메모리 사용량이 임계치를 초과하면 혼잡 상태로 판단")
	void isMemoryUsageExceed_over_threshold_returnTrue() {
		// given: Redis의 최대 메모리를 100MB로 제한, 70MB의 데이터를 삽입하여 임계치 70% 넘기기
		String tmp = "A".repeat(1 * 1024 * 1024);
		for (int i = 1; i <= 70; i++) {
			redisTemplate.opsForValue().set("tmp" + i, tmp); // 대략 70 + a MB (실제 문자열 + 구조체 + 기타 오버헤드 ...)
		}

		// when
		boolean result = redisCongestionManager.isMemoryUsageExceeded();

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("isMemoryUsageExceeded() 메모리 사용량이 임계치 이하이면 혼잡 상태로 판단하지 않음")
	void isMemoryUsageExceed_under_threshold_returnFalse() {
		// given
		String smallValue = "A".repeat(1 * 1024 * 1024); // 약 1MB
		redisTemplate.opsForValue().set("small-key", smallValue);

		// when
		boolean result = redisCongestionManager.isMemoryUsageExceeded();

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("isStreamLengthExceeded() 스트림의 길이가 임계치를 초과하면 혼잡 상태로 판단")
	void isStreamLengthExceeded_over_threshold_returnTrue() {
		// given: 스트림 길이 임계치가 30
		IntStream.range(0, 31).forEach(i ->
			redisTemplate.opsForStream().add(TEST_STREAM_KEY, Map.of("key", "value" + i))
		);

		// when
		boolean result = redisCongestionManager.isStreamLengthExceeded(TEST_STREAM_KEY);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("isStreamLengthExceeded() 스트림의 길이가 임계치 이하이면 혼잡 상태로 판단하지 않음")
	void isStreamLengthExceeded_under_threshold_returnFalse() {
		// given
		IntStream.range(0, 10).forEach(i ->
			redisTemplate.opsForStream().add(TEST_STREAM_KEY, Map.of("key", "value" + i))
		);

		// when
		boolean result = redisCongestionManager.isStreamLengthExceeded(TEST_STREAM_KEY);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("isPendingMessagesExceeded() Pending 메시지 개수가 임계치를 초과하면 혼잡 상태로 판단")
	void isPendingMessagesExceeded_over_threshold_returnTrue() {
		// given: Pending 임계치 10개
		IntStream.range(0, 11).forEach(i ->
			redisTemplate.opsForStream().add(TEST_STREAM_KEY, Map.of("key", "value" + i))
		);
		redisTemplate.opsForStream().read(
			Consumer.from(TEST_GROUP_NAME, TEST_CONSUMER_NAME),
			StreamOffset.create(TEST_STREAM_KEY, ReadOffset.from(">")) // 아직 소비하지 않은
		);

		// when
		boolean result = redisCongestionManager.isPendingMessagesExceeded(TEST_STREAM_KEY, TEST_GROUP_NAME);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("isPendingMessagesExceeded() Pending 메시지 개수가 임계치 이하이면 혼잡 상태로 판단하지 않음")
	void isPendingMessagesExceeded_under_threshold_returnFalse() {
		// given
		IntStream.range(0, 5).forEach(i ->
			redisTemplate.opsForStream().add(TEST_STREAM_KEY, Map.of("key", "value" + i))
		);
		redisTemplate.opsForStream().read(
			Consumer.from(TEST_GROUP_NAME, TEST_CONSUMER_NAME),
			StreamOffset.create(TEST_STREAM_KEY, ReadOffset.from(">"))
		);

		// when
		boolean result = redisCongestionManager.isPendingMessagesExceeded(TEST_STREAM_KEY, TEST_GROUP_NAME);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("isCongested() 모두 만족하지 않으면 혼잡 상태로 판단하지 않음")
	void isCongested_all_conditions_returnFalse() {
		// given
		redisTemplate.opsForValue().set("small-key", "small-value");
		IntStream.range(0, 5).forEach(i ->
			redisTemplate.opsForStream().add(TEST_STREAM_KEY, Map.of("key", "value" + i))
		);

		// when
		boolean isCongested = redisCongestionManager.isCongested(TEST_STREAM_KEY, TEST_GROUP_NAME);

		// then
		assertThat(isCongested).isFalse();
	}
}