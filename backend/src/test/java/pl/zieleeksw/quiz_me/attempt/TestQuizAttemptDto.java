package pl.zieleeksw.quiz_me.attempt;

import java.time.Instant;

public record TestQuizAttemptDto(
        Long id,
        Long courseId,
        Long quizId,
        Long userId,
        String quizTitle,
        int correctAnswers,
        int totalQuestions,
        Instant finishedAt
) {
}
