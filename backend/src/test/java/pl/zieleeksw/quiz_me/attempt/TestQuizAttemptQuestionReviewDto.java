package pl.zieleeksw.quiz_me.attempt;

import java.util.List;

public record TestQuizAttemptQuestionReviewDto(
        Long questionId,
        String prompt,
        Long selectedAnswerId,
        Long correctAnswerId,
        boolean answeredCorrectly,
        List<TestQuizAttemptAnswerReviewDto> answers
) {
}
