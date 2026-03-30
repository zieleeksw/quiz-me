package pl.zieleeksw.quiz_me.question;

import java.time.Instant;
import java.util.List;

public record TestQuestionVersionDto(
        Long id,
        Long questionId,
        int versionNumber,
        Instant createdAt,
        String prompt,
        List<TestQuestionAnswerDto> answers
) {
}
