package shop.buzzle.buzzle.quiz.api.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;
import shop.buzzle.buzzle.global.dto.PageInfoResDto;
import shop.buzzle.buzzle.quiz.domain.QuizResult;

import java.util.List;

@Builder
public record QuizResultSimplePageResDto(
        List<QuizResultSimpleResDto> content,
        PageInfoResDto pageInfo
) {
    public static QuizResultSimplePageResDto from(Page<QuizResult> quizResultPage) {
        List<QuizResultSimpleResDto> content = quizResultPage.getContent().stream()
                .map(QuizResultSimpleResDto::from)
                .toList();

        return QuizResultSimplePageResDto.builder()
                .content(content)
                .pageInfo(PageInfoResDto.from(quizResultPage))
                .build();
    }
}