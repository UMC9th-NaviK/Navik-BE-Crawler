package navik.redis.client;

import java.util.Objects;

import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
		try {
			// 객체를 JSON 문자열로 변환
			String recruitmentJson = objectMapper.writeValueAsString(recruitment);

			// String 타입으로 레코드 생성
			ObjectRecord<String, String> record = StreamRecords.newRecord()
				.ofObject(recruitmentJson)
				.withStreamKey(streamKey);

			RecordId recordId = redisTemplate.opsForStream()
				.add(record, RedisStreamCommands.XAddOptions.maxlen(200)); // 200건;
			if (Objects.nonNull(recordId))
				log.info("[RedisStreamProducer] 채용 공고 발행 성공 recordId = {}", recordId.getValue());
			else
				log.error("[RedisStreamProducer] 채용 공고 발행 실패");
		} catch (Exception e) {
			log.error("[RedisStreamProducer] JSON 변환 실패");
		}
	}
}
