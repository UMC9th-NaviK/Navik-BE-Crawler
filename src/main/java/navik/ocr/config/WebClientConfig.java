package navik.ocr.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient webClient() {
		// 1. HttpClient 설정
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
			.responseTimeout(Duration.ofSeconds(10))
			.doOnConnected(conn ->
				conn.addHandlerLast(new ReadTimeoutHandler(10))
					.addHandlerLast(new WriteTimeoutHandler(10)))
			.compress(true)
			.keepAlive(true);

		// 2. 메모리 버퍼 2MB 확장
		ExchangeStrategies strategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
			.build();

		// 3. WebClient 빌드
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.exchangeStrategies(strategies)
			.defaultHeader(HttpHeaders.USER_AGENT,
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
			.build();
	}
}
