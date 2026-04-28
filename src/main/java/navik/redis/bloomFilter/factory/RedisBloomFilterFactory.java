package navik.redis.bloomFilter.factory;

import java.util.Collection;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import navik.redis.bloomFilter.wrapper.RedisBloomFilter;

@Component
@RequiredArgsConstructor
public class RedisBloomFilterFactory {

	private final RedissonClient redissonClient;

	/**
	 * 필터 이름 기반 단순 조회용
	 */
	public RedisBloomFilter getBloomFilter(String filterName) {
		RBloomFilter<String> filter = redissonClient.getBloomFilter(filterName);
		return new RedisBloomFilter(filter);
	}

	/**
	 * 필터 생성 및 초기 데이터 적재용
	 */
	public RedisBloomFilter createBloomFilter(
		String filterName,
		long insertionSize,
		Collection<String> values,
		double fpp
	) {

		// 1. 필터 get
		RedisBloomFilter redisBloomFilter = getBloomFilter(filterName);

		// 2. 최초 필터인 경우 초기화 (이미 존재하면 초기화되지 않음)
		redissonClient.getBloomFilter(filterName).tryInit(insertionSize, fpp);

		// 3. 초기 데이터 적재
		if (values != null && !values.isEmpty()) {
			for (String v : values) {
				redisBloomFilter.put(v);
			}
		}

		// 4. return
		return redisBloomFilter;
	}
}
