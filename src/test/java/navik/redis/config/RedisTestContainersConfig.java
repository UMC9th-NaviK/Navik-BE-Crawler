package navik.redis.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public abstract class RedisTestContainersConfig {

	private static final String REDIS_DOCKER_IMAGE = "redis:latest";
	private static final int REDIS_PORT = 6379;
	private static final GenericContainer<?> REDIS_CONTAINER;

	static {
		REDIS_CONTAINER = new GenericContainer<>(REDIS_DOCKER_IMAGE) // redis docker image
			.withExposedPorts(REDIS_PORT) // 컨테이너에서 노출할 포트
			.withReuse(Boolean.TRUE) // 컨테이너 재사용 여부
			.waitingFor(Wait.forListeningPort()) // redis 실행될 때 까지 대기
			.withCommand("redis-server", "--maxmemory", "100mb", "--maxmemory-policy",
				"allkeys-lru"); // maxmemory 100mb로 설정

		REDIS_CONTAINER.start(); // 테스트 실행 시 redis 컨테이너 자동 시작
	}

	@DynamicPropertySource
	public static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(REDIS_PORT).toString());
	}
}
