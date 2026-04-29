package navik.redis.bloomFilter.wrapper;

import org.redisson.api.RBloomFilter;

// Wrapper
public class RedisBloomFilter {

	private final RBloomFilter<String> bloomFilter;

	public RedisBloomFilter(RBloomFilter<String> bloomFilter) {
		this.bloomFilter = bloomFilter;
	}

	public void put(String key) {
		bloomFilter.add(key);
	}

	public boolean mightContain(String key) {
		return bloomFilter.contains(key);
	}
}

