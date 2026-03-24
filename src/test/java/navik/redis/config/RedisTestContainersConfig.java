package navik.redis.config;

import static org.testcontainers.utility.DockerImageName.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration
public class RedisTestContainersConfig {

	private static final String REDIS_DOCKER_IMAGE = "redis:7.4.1-alpine3.20";

	static {
		GenericContainer<?> REDIS_CONTAINER =
			new GenericContainer<>(parse(REDIS_DOCKER_IMAGE))
				.withExposedPorts(6379)
				.withReuse(true);

		REDIS_CONTAINER.start();

		System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
		System.setProperty("spring.data.redis.port", REDIS_CONTAINER.getMappedPort(6379).toString());
	}
}
