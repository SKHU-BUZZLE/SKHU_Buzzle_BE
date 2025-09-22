package shop.buzzle.buzzle.quiz.api.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;
import shop.buzzle.buzzle.global.dto.PageInfoResDto;
import shop.buzzle.buzzle.quiz.domain.QuizResult;

import java.util.List;

@Builder
public record QuizResultPageResDto(
        List<QuizResultResDto> content,
        PageInfoResDto pageInfo
) {
    public static QuizResultPageResDto from(Page<QuizResult> quizResultPage) {
        List<QuizResultResDto> content = quizResultPage.getContent().stream()
                .map(QuizResultResDto::from)
                .toList();

        return QuizResultPageResDto.builder()
                .content(content)
                .pageInfo(PageInfoResDto.from(quizResultPage))
                .build();
    }
}