package pl.zieleeksw.quiz_me.quiz;

import java.time.Instant;
import java.util.List;

public record TestQuizVersionDto(
        Long id,
        Long quizId,
        int versionNumber,
        Instant createdAt,
        String title,
        String mode,
        Integer randomCount,
        String questionOrder,
        String answerOrder,
        List<Long> questionIds,
        List<TestQuizCategoryDto> categories
) {
}
