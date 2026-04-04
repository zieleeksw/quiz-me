package pl.zieleeksw.quiz_me.quiz;

import java.time.Instant;
import java.util.List;

public record QuizVersionDto(
        Long id,
        Long quizId,
        int versionNumber,
        Instant createdAt,
        String title,
        QuizMode mode,
        Integer randomCount,
        QuizOrderMode questionOrder,
        QuizOrderMode answerOrder,
        List<Long> questionIds,
        List<QuizCategoryDto> categories
) {
}
