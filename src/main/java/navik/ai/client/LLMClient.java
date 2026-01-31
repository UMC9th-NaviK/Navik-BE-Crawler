package navik.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import navik.ai.dto.LLMResponseDTO;

@Service
@RequiredArgsConstructor
public class LLMClient {

	private final ChatClient chatClient;

	public LLMResponseDTO.Recruitment getRecruitment(String text) {

		SystemMessage systemMessage = SystemMessage.builder()
			.text("""
				[역할 부여]
				당신은 채용 공고를 분석하여 직무 별로 요구되는 핵심 역량(KPI)을 추출하는 전문가입니다.
				직무 핵심 역량(KPI)이란 '담당 업무', '요구 사항', '우대 사항' 등에서 '능력', '역량', '경험', '이해도'와 같은 맥락을 의미합니다.
				[옳은 예시]: 'RESTful API 개발 및 설계 능력', '비동기 메시지 큐 사용 경험', 'MSA 아키텍처에 대한 이해도'
				[잘못된 예시]: '계획성', '성실성', '협동성', '꼼꼼함', '윤리의식'
				
				[지시]
				1. 우선 OCR 내용을 구조화하세요.
				2. '프로덕트 매니저', '프로덕트 디자이너', '프론트엔드 개발자', '백엔드 개발자'로 분류 가능한 직무만 남기세요.
				3. 직무 별로 특정 학력과 특정 전공이 필수 요건인지 판단하세요.
				4. 아래 규칙을 참고하여 유효한 JSON을 반환하세요.
				
				[규칙]
				- 직무명은 있는 그대로 작성한다.
				- 모든 KPI는 서술형 문장으로 작성한다.
				- 추상적이거나 감성적인 표현은 사용하지 않는다.
				- 시작일과 마감일에 타임존은 포함하지 않는다.
				- 공고 마감일은 현재 시간을 고려하여 계산한다. 상시 채용이라면 null로 출력한다.
				- 특정 학력이 필수 요건으로 나타난 경우 educationType에 출력한다. 필수가 아니라면 null로 출력한다.
				- 특정 전공이 필수 요건으로 나타난 경우 majorType에 출력한다. 필수가 아니라면 null로 출력한다.
				- 회사 로고가 없는 경우 null로 출력한다.
				- 그 밖에 정보가 없는 경우 null로 출력한다.
				- 요약은 최대 3문장의 친근한 말투로 줄바꿈하여 작성한다.
				- 제목은 필수로 출력한다.
				""")
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
