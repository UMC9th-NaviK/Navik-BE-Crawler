package navik.growth.config;

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
public class NotionWebClientConfig {

	@Bean("notionWebClient")
	public WebClient notionWebClient() {
		// 1. HttpClient 설정 (노션용 헤더 크기 확장)
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
			.responseTimeout(Duration.ofSeconds(30))
			.doOnConnected(conn ->
				conn.addHandlerLast(new ReadTimeoutHandler(30))
					.addHandlerLast(new WriteTimeoutHandler(30)))
			.httpResponseDecoder(spec -> spec.maxHeaderSize(32 * 1024)) // 32KB 헤더 허용
			.compress(true)
			.followRedirect(true);

		// 2. 메모리 버퍼 10MB 확장 (노션 페이지는 큼)
		ExchangeStrategies strategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
			.build();

		// 3. WebClient 빌드 (Notion API용)
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.exchangeStrategies(strategies)
			.defaultHeader(HttpHeaders.ACCEPT, "application/json")
			.build();
	}
}
