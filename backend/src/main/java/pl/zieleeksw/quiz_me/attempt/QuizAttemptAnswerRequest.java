package pl.zieleeksw.quiz_me.attempt;

import jakarta.validation.constraints.NotNull;

public record QuizAttemptAnswerRequest(
        @NotNull(message = "Question id is required.")
        Long questionId,
        @NotNull(message = "Answer id is required.")
        Long answerId
) {
}
