package pl.zieleeksw.quiz_me.attempt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateQuizSessionRequest(
        @NotNull(message = "Current question index is required.")
        Integer currentIndex,
        List<@Valid QuizAttemptAnswerRequest> answers
) {
}
