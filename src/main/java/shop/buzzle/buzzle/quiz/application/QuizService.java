package shop.buzzle.buzzle.quiz.application;

import java.util.ArrayList;
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
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizAnswerReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.RetryQuizAnswerReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResListDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResultResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.RetryQuizResDto;
import shop.buzzle.buzzle.quiz.domain.QuizResult;
import shop.buzzle.buzzle.quiz.domain.QuizScore;
import shop.buzzle.buzzle.quiz.domain.repository.QuizResultRepository;
import shop.buzzle.buzzle.quiz.exception.QuizResultNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final ChatClient chatClient;
    private final MemberRepository memberRepository;
    private final QuizResultRepository quizResultRepository;

    @Value("${questions.all}")
    private String allQuestions;

    @Value("${questions.history}")
    private String historyQuestions;

    @Value("${questions.society}")
    private String societyQuestions;

    @Value("${questions.science}")
    private String scienceQuestions;

    @Value("${questions.culture}")
    private String cultureQuestions;

    @Value("${questions.sports}")
    private String sportsQuestions;

    @Value("${questions.nature}")
    private String natureQuestions;

    @Value("${questions.misc}")
    private String miscQuestions;

    @Transactional
    public QuizResListDto askForAdvice(QuizSizeReqDto quizSizeReqDto) {

        String selectedQuestions = switch (quizSizeReqDto.category()) {
            case ALL -> allQuestions;
            case HISTORY -> historyQuestions;
            case SOCIETY -> societyQuestions;
            case SCIENCE -> scienceQuestions;
            case CULTURE -> cultureQuestions;
            case SPORTS -> sportsQuestions;
            case NATURE -> natureQuestions;
            case MISC -> miscQuestions;
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
        log.info("Raw Quiz Text: {}", quizText);

        String question = "";
        String option1 = "";
        String option2 = "";
        String option3 = "";
        String option4 = "";
        String answer = "";

        if (quizText.contains(" , ")) {
            String cleaned = quizText.replaceAll("^\\[|]$", ""); // [와 ] 제거
            String[] parts = cleaned.split(" , ");

            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("문제:")) question = part.replace("문제:", "").trim();
                else if (part.startsWith("1.")) option1 = part.replace("1.", "").trim();
                else if (part.startsWith("2.")) option2 = part.replace("2.", "").trim();
                else if (part.startsWith("3.")) option3 = part.replace("3.", "").trim();
                else if (part.startsWith("4.")) option4 = part.replace("4.", "").trim();
                else if (part.startsWith("정답:")) answer = part.replace("정답:", "").trim();
            }
        } else {
            // 줄바꿈 기반 포맷 처리
            String[] lines = quizText.split("\\r?\\n");

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("문제:")) question = line.replace("문제:", "").trim();
                else if (line.startsWith("1.")) option1 = line.replace("1.", "").trim();
                else if (line.startsWith("2.")) option2 = line.replace("2.", "").trim();
                else if (line.startsWith("3.")) option3 = line.replace("3.", "").trim();
                else if (line.startsWith("4.")) option4 = line.replace("4.", "").trim();
                else if (line.startsWith("정답:")) answer = line.replace("정답:", "").trim();
            }
        }

        log.info("Parsed -> Q: {}, 1: {}, 2: {}, 3: {}, 4: {}, Answer: {}", question, option1, option2, option3, option4, answer);

        return QuizResDto.of(question, option1, option2, option3, option4, answer);
    }

    @Transactional
    public boolean chooseTheCorrectAnswer(String email, QuizAnswerReqDto quizAnswerReqDto) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        QuizResult quizResult = QuizResult.createResult(
                member,
                quizAnswerReqDto.question(),
                quizAnswerReqDto.option1(),
                quizAnswerReqDto.option2(),
                quizAnswerReqDto.option3(),
                quizAnswerReqDto.option4(),
                quizAnswerReqDto.correctAnswerNumber(),
                quizAnswerReqDto.userAnswerNumber(),
                quizAnswerReqDto.category()
        );

        quizResultRepository.save(quizResult);

        if (quizResult.getIsCorrect()) {
            member.incrementStreak(QuizScore.PERSONAL_SCORE.getScore());
        } else {
            member.decrementLife();
        }

        return quizResult.getIsCorrect();
    }

    @Transactional(readOnly = true)
    public List<QuizResultResDto> getIncorrectAnswers(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        List<QuizResult> incorrectResults = quizResultRepository.findIncorrectAnswersByMember(member);

        return incorrectResults.stream()
                .map(QuizResultResDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RetryQuizResDto getRetryQuiz(String email, Long quizResultId) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);
        
        QuizResult quizResult = quizResultRepository.findById(quizResultId)
                .orElseThrow(QuizResultNotFoundException::new);

        if (!quizResult.getMember().equals(member)) {
            throw new IllegalArgumentException("해당 퀴즈 결과는 사용자의 것이 아닙니다.");
        }

        if (quizResult.getIsCorrect()) {
            throw new IllegalStateException("올바르게 맞춘 문제는 다시 풀 수 없습니다.");
        }

        return RetryQuizResDto.from(quizResult);
    }

    @Transactional
    public boolean retryIncorrectQuiz(String email, RetryQuizAnswerReqDto retryQuizAnswerReqDto) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);
        
        QuizResult originalQuizResult = quizResultRepository.findById(retryQuizAnswerReqDto.originalQuizId())
                .orElseThrow(QuizResultNotFoundException::new);

        if (!originalQuizResult.getMember().equals(member)) {
            throw new IllegalArgumentException("해당 퀴즈 결과는 사용자의 것이 아닙니다.");
        }

        if (originalQuizResult.getIsCorrect()) {
            throw new IllegalStateException("올바르게 맞춘 문제는 다시 풀 수 없습니다.");
        }

        originalQuizResult.updateUserAnswer(retryQuizAnswerReqDto.userAnswerNumber());

        return originalQuizResult.getIsCorrect();
    }

    @Transactional
    public void deleteIncorrectQuiz(String email, Long quizResultId) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);
        
        QuizResult quizResult = quizResultRepository.findById(quizResultId)
                .orElseThrow(QuizResultNotFoundException::new);

        if (!quizResult.getMember().equals(member)) {
            throw new IllegalArgumentException("해당 퀴즈 결과는 사용자의 것이 아닙니다.");
        }

        quizResultRepository.delete(quizResult);
    }
}