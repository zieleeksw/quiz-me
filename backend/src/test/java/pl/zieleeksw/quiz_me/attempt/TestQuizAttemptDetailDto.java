package pl.zieleeksw.quiz_me.attempt;

import java.time.Instant;
import java.util.List;

public record TestQuizAttemptDetailDto(
        Long id,
        Long courseId,
        Long quizId,
        Long userId,
        String quizTitle,
        int correctAnswers,
        int totalQuestions,
        Instant finishedAt,
        List<TestQuizAttemptQuestionReviewDto> questions
) {
}
