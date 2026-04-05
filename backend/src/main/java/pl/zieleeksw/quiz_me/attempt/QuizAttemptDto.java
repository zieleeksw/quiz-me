package pl.zieleeksw.quiz_me.attempt;

import java.time.Instant;

public record QuizAttemptDto(
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
