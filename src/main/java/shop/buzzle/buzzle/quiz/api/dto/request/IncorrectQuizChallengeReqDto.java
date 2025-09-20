package shop.buzzle.buzzle.quiz.api.dto.request;

import java.util.List;

public record IncorrectQuizChallengeReqDto(
        List<IncorrectQuizAnswerDto> answers
) {
    
    public record IncorrectQuizAnswerDto(
            Long quizResultId,
            String userAnswerNumber
    ) {}
}