package pl.zieleeksw.quiz_me.question;

import java.time.Instant;
import java.util.List;

public record QuestionVersionDto(
        Long id,
        Long questionId,
        int versionNumber,
        Instant createdAt,
        String prompt,
        List<QuestionCategoryDto> categories,
        List<QuestionAnswerDto> answers
) {
}
