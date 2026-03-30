package pl.zieleeksw.quiz_me.question;

import java.time.Instant;
import java.util.List;

public record QuestionDto(
        Long id,
        Long courseId,
        int currentVersionNumber,
        Instant createdAt,
        Instant updatedAt,
        String prompt,
        List<QuestionAnswerDto> answers
) {
}
