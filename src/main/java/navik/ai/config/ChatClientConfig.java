package navik.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ChatClientConfig {
	@Bean
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.defaultAdvisors(
				new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
			)
			.build();
	}
}
