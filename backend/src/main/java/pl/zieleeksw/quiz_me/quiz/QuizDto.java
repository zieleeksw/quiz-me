package pl.zieleeksw.quiz_me.quiz;

import java.time.Instant;
import java.util.List;

public record QuizDto(
        Long id,
        Long courseId,
        int currentVersionNumber,
        Instant createdAt,
        Instant updatedAt,
        String title,
        QuizMode mode,
        Integer randomCount,
        QuizOrderMode questionOrder,
        QuizOrderMode answerOrder,
        List<Long> questionIds,
        List<QuizCategoryDto> categories
) {
}
