package shop.buzzle.buzzle.quiz.api.dto.request;

public record RetryQuizAnswerReqDto(
        Long originalQuizId,
        String userAnswerNumber
) {
}