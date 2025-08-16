package shop.buzzle.buzzle.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.buzzle.buzzle.ai.dto.AiResponseDto;

@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;

    // AI에게 조언 구하기
    @Transactional
    public AiResponseDto askForAdvice(){
        ChatResponse response = callChat();
        if (response == null) {
            response = callChat();
        }

        return AiResponseDto.builder()
                .answer(response.getResult().getOutput().getContent()).build();

    }

    // AI 응답 메서드
    private ChatResponse callChat() {
        return chatClient.call(
                new Prompt(
                        ("너 gpt 버전 몇이야? 3.5야 4이야. 4라면 4o야 아니면 mini야?"
                        ),
                        OpenAiChatOptions.builder()
                                .withTemperature(0.4F)
                                .withFrequencyPenalty(0.7F)
                                .withModel("gpt-4o")
                                .build()
                ));
    }
}
