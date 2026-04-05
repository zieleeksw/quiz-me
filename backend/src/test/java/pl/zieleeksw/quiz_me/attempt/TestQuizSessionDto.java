package pl.zieleeksw.quiz_me.attempt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TestQuizSessionDto(
        Long id,
        Long courseId,
        Long quizId,
        Long userId,
        String quizTitle,
        List<Long> questionIds,
        int currentIndex,
        Map<Long, Long> answers,
        Instant updatedAt
) {
}
