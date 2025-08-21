package shop.buzzle.buzzle.quiz.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResListDto;
import shop.buzzle.buzzle.quiz.application.QuizService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController implements QuizDocs{

    private final QuizService quizService;

    // AI 응답
    @PostMapping
    public RspTemplate<QuizResDto> advice(@CurrentUserEmail String email
            , @RequestBody QuizReqDto quizReqDto) {
        return new RspTemplate<>(HttpStatus.OK, "퀴즈 생성 완료", quizService.askForAdvice(email, quizReqDto));
    }

    // 퀴즈 여러 개 생성
    @PostMapping("/multiple")
    public RspTemplate<QuizResListDto> generateMultipleQuizzes(@CurrentUserEmail String email,
                                                               @RequestBody QuizSizeReqDto quizSizeReqDto) {
        return new RspTemplate<>(HttpStatus.OK,
                "퀴즈 여러 개 생성 완료",
                quizService.askForAdvice(quizSizeReqDto));
    }

    @GetMapping("/correct-answer")
    public RspTemplate<Boolean> chooseCorrectAnswer(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "정답 선택 완료", quizService.chooseTheCorrectAnswer(email));
    }

    @GetMapping("/incorrect-answer")
    public RspTemplate<Boolean> chooseIncorrectAnswer(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "오답 선택 완료", quizService.chooseTheIncorrectAnswer(email));
    }
}