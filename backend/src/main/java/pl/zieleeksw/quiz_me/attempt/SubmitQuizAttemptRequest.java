package pl.zieleeksw.quiz_me.attempt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitQuizAttemptRequest(
        @NotEmpty(message = "Attempt answers are required.")
        List<@Valid QuizAttemptAnswerRequest> answers
) {
}
