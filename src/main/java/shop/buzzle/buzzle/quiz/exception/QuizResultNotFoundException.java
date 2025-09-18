package shop.buzzle.buzzle.quiz.exception;

import org.springframework.http.HttpStatus;
import shop.buzzle.buzzle.global.error.exception.NotFoundGroupException;

public class QuizResultNotFoundException extends NotFoundGroupException {
    public QuizResultNotFoundException(String message) {
        super(message);
    }
    public QuizResultNotFoundException() {
        this("존재하지 않는 퀴즈 결과입니다.");
    }
}