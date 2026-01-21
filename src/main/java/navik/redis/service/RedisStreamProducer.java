package navik.redis.service;

import java.util.Objects;

import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import navik.crawler.dto.Recruitment;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamProducer {

	private final RedisTemplate<String, Object> redisTemplate;

	public void produceRecruitment(String streamKey, Recruitment recruitment) {
		ObjectRecord<String, Recruitment> record = StreamRecords.newRecord()
			.ofObject(recruitment)
			.withStreamKey(streamKey);

		RecordId recordId = redisTemplate.opsForStream().add(record); // MKSTREAM 포함
		if (Objects.isNull(recordId)) {
			log.error("[RedisStreamProducer] 채용 공고 발행 실패");
		}
		log.info("[RedisStreamProducer] 채용 공고 발행 성공");
	}
}
