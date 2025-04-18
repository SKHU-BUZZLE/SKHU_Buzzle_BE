package shop.itcontest17.itcontest17.quiz.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizReqDto;
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizSizeReqDto;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResDto;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResListDto;
import shop.itcontest17.itcontest17.quiz.domain.QuizCategory;
import shop.itcontest17.itcontest17.quiz.domain.QuizScore;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final ChatClient chatClient;
    private final MemberRepository memberRepository;

    @Value("${questions.history}")
    private String historyQuestions;

    @Value("${questions.fouridioms}")
    private String fouridiomsQuestions;

    @Value("${questions.capital}")
    private String capitalQuestions;

    @Value("${questions.science}")
    private String scienceQuestions;

    @Value("${questions.all}")
    private String allQuestions;

    @Transactional
    public QuizResDto askForAdvice(String email, QuizReqDto quizReqDto) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        String selectedQuestions = switch (QuizCategory.valueOf(quizReqDto.category().name())) {
            case HISTORY -> historyQuestions;
            case FOUR_IDIOMS -> fouridiomsQuestions;
            case CAPITAL -> capitalQuestions;
            case SCIENCE -> scienceQuestions;
            case ALL -> allQuestions;
        };

        ChatResponse response = callChat(selectedQuestions);
        if (response == null) {
            response = callChat(selectedQuestions);
        }

        return parseQuiz(response.getResult().getOutput().getContent());
    }

    // 10개 퀴즈 만들기
    @Transactional
    public QuizResListDto askForAdvice(QuizSizeReqDto quizSizeReqDto) {

        String selectedQuestions = switch (QuizCategory.valueOf(quizSizeReqDto.category().name())) {
            case HISTORY -> historyQuestions;
            case FOUR_IDIOMS -> fouridiomsQuestions;
            case CAPITAL -> capitalQuestions;
            case SCIENCE -> scienceQuestions;
            case ALL -> allQuestions;
        };

        List<QuizResDto> quizList = new ArrayList<>();

        for (int i = 0; i < quizSizeReqDto.size(); i++) {
            ChatResponse response = callChat(selectedQuestions);
            if (response == null) {
                response = callChat(selectedQuestions);
            }
            quizList.add(parseQuiz(response.getResult().getOutput().getContent()));
        }

        return QuizResListDto.from(quizList);
    }

    private ChatResponse callChat(String prompt) {
        return chatClient.call(
                new Prompt(
                        prompt,
                        OpenAiChatOptions.builder()
                                .withTemperature(1F)
                                .withFrequencyPenalty(0.6F)
                                .withPresencePenalty(1F)
                                .withModel("gpt-4o")
                                .build()
                ));
    }

    private QuizResDto parseQuiz(String quizText) {
        // 문자열을 줄바꿈으로 분리
        String[] lines = quizText.split("\n");

        // 문제, 선택지, 정답을 파싱
        String question = lines[0].replace("문제: ", "").trim();
        String option1 = lines[2].replace("1. ", "").trim();
        String option2 = lines[3].replace("2. ", "").trim();
        String option3 = lines[4].replace("3. ", "").trim();
        String option4 = lines[5].replace("4. ", "").trim();

        String answer = (lines.length > 6 && !lines[6].replace("정답: ", "").trim().isEmpty())
                ? lines[6].replace("정답: ", "").trim()
                : (lines.length > 7 ? lines[7].replace("정답: ", "").trim() : "");
        log.info(Arrays.stream(lines).toList().toString());
        // DTO 생성 및 반환
        return QuizResDto.of(question, option1, option2, option3, option4, answer);
    }

    @Transactional
    public boolean chooseTheCorrectAnswer(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        member.incrementStreak(QuizScore.PERSONAL_SCORE.getScore());

        return true;
    }

    @Transactional
    public boolean chooseTheIncorrectAnswer(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        member.decrementLife();

        return true;
    }
}