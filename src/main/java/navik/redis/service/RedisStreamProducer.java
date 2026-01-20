package navik.redis.service;

import java.util.Map;
import java.util.Objects;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.crawler.dto.Recruitment;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamProducer {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	public void produceRecruitment(String streamKey, Recruitment recruitment) {
		Map<String, String> recruitmentMap = objectMapper.convertValue(recruitment,
			new TypeReference<Map<String, String>>() {  // 제네릭 타입 보존
			});
		log.info("[RedisStreamProducer] 공고 Map: {}", recruitmentMap);

		// Value로 Map을 담을 수 있는 Record
		MapRecord<String, String, String> record = MapRecord.create(streamKey, recruitmentMap);

		RecordId recordId = redisTemplate.opsForStream().add(record);
		if (Objects.isNull(recordId)) {
			log.error("[RedisStreamProducer] 채용 공고 저장 실패");
		}
	}
}
