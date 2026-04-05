package pl.zieleeksw.quiz_me.attempt;

import java.util.List;

public record QuizAttemptQuestionReviewDto(
        Long questionId,
        String prompt,
        Long selectedAnswerId,
        Long correctAnswerId,
        boolean answeredCorrectly,
        List<QuizAttemptAnswerReviewDto> answers
) {
}
