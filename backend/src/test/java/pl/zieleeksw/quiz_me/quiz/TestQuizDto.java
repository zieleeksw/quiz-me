package pl.zieleeksw.quiz_me.quiz;

import java.time.Instant;
import java.util.List;

public record TestQuizDto(
        Long id,
        Long courseId,
        int currentVersionNumber,
        Instant createdAt,
        Instant updatedAt,
        String title,
        String mode,
        Integer randomCount,
        String questionOrder,
        String answerOrder,
        List<Long> questionIds,
        List<TestQuizCategoryDto> categories
) {
}
