package navik.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import navik.ai.dto.LLMResponseDTO;
import navik.ai.util.PromptLoader;

@Service
@RequiredArgsConstructor
public class LLMClient {

	private final ChatClient chatClient;
	private final PromptLoader promptLoader;

	private static final String RECRUITMENT_SYSTEM_PROMPT_PATH = "classpath:prompts/recruitment/system-prompt.txt";

	public LLMResponseDTO.Recruitment getRecruitment(String text) {

		String systemPromptText = promptLoader.loadPrompt(RECRUITMENT_SYSTEM_PROMPT_PATH);

		SystemMessage systemMessage = SystemMessage.builder()
			.text(systemPromptText)
			.build();

		UserMessage userMessage = UserMessage.builder()
			.text(text)
			.build();

		return chatClient.prompt()
			.messages(systemMessage, userMessage)
			.options(ChatOptions.builder()
				.temperature(0.0)
				.build()
			)
			.call()
			.entity(LLMResponseDTO.Recruitment.class);
	}
}
