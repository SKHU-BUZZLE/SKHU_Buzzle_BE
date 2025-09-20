package shop.buzzle.buzzle.quiz.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizAnswerReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.RetryQuizAnswerReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResListDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResultResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.RetryQuizResDto;
import shop.buzzle.buzzle.quiz.api.dto.request.IncorrectQuizChallengeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.IncorrectQuizChallengeResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.IncorrectQuizChallengeResultResDto;
import java.util.List;
import shop.buzzle.buzzle.quiz.application.QuizService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController implements QuizDocs{

    private final QuizService quizService;

    @PostMapping("/multiple")
    public RspTemplate<QuizResListDto> generateMultipleQuizzes(@CurrentUserEmail String email,
                                                               @RequestBody QuizSizeReqDto quizSizeReqDto) {
        return new RspTemplate<>(HttpStatus.OK,
                "퀴즈 여러 개 생성 완료",
                quizService.askForAdvice(quizSizeReqDto));
    }

    @PostMapping("/answer")
    public RspTemplate<Boolean> submitAnswer(@CurrentUserEmail String email,
                                           @RequestBody QuizAnswerReqDto quizAnswerReqDto) {
        boolean isCorrect = quizService.chooseTheCorrectAnswer(email, quizAnswerReqDto);
        String message = isCorrect ? "정답입니다!" : "오답입니다.";
        return new RspTemplate<>(HttpStatus.OK, message, isCorrect);
    }

    @GetMapping("/incorrect-notes")
    public RspTemplate<List<QuizResultResDto>> getIncorrectNotes(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "오답노트 조회 완료", quizService.getIncorrectAnswers(email));
    }

    @GetMapping("/incorrect-notes/{quizResultId}")
    public RspTemplate<RetryQuizResDto> getIncorrectQuizDetail(@CurrentUserEmail String email, 
                                                             @PathVariable Long quizResultId) {
        return new RspTemplate<>(HttpStatus.OK, "오답 문제 상세 조회 완료", quizService.getIncorrectQuizDetail(email, quizResultId));
    }

    @PostMapping("/incorrect-notes/retry")
    public RspTemplate<Boolean> retryIncorrectQuiz(@CurrentUserEmail String email,
                                                 @RequestBody RetryQuizAnswerReqDto retryQuizAnswerReqDto) {
        boolean isCorrect = quizService.retryIncorrectQuiz(email, retryQuizAnswerReqDto);
        String message = isCorrect ? "재도전 성공! 정답입니다!" : "재도전했지만 아직 오답입니다.";
        return new RspTemplate<>(HttpStatus.OK, message, isCorrect);
    }

    @DeleteMapping("/incorrect-notes/{quizResultId}")
    public RspTemplate<Void> deleteIncorrectQuiz(@CurrentUserEmail String email,
                                               @PathVariable Long quizResultId) {
        quizService.deleteIncorrectQuiz(email, quizResultId);
        return new RspTemplate<>(HttpStatus.OK, "오답노트에서 삭제 완료", null);
    }

    @GetMapping("/incorrect-notes/challenge")
    public RspTemplate<IncorrectQuizChallengeResDto> getIncorrectQuizChallenge(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "오답 재도전 문제 조회 완료", quizService.getIncorrectQuizChallenge(email));
    }

    @PostMapping("/incorrect-notes/challenge")
    public RspTemplate<IncorrectQuizChallengeResultResDto> submitIncorrectQuizChallenge(@CurrentUserEmail String email,
                                                                                      @RequestBody IncorrectQuizChallengeReqDto request) {
        return new RspTemplate<>(HttpStatus.OK, "오답 재도전 완료", quizService.submitIncorrectQuizChallenge(email, request));
    }
}