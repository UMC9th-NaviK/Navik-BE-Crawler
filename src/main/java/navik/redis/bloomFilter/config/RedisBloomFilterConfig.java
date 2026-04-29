package navik.redis.bloomFilter.config;

import java.util.Collections;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import navik.crawler.constants.JobKoreaConstant;
import navik.redis.bloomFilter.factory.RedisBloomFilterFactory;

@Configuration
public class RedisBloomFilterConfig {

	@Bean
	public ApplicationRunner initBloomFilters(RedisBloomFilterFactory factory) {
		return args -> {
			factory.createBloomFilter(
				JobKoreaConstant.BLOOM_FILTER_NAME,
				JobKoreaConstant.BLOOM_FILTER_INSERTION_SIZE,
				Collections.emptyList(),
				JobKoreaConstant.BLOOM_FILTER_FPP
			);
		};
	}
}
