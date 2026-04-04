package pl.zieleeksw.quiz_me.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuizRequest(
        @NotBlank(message = "Quiz title is required.")
        @Size(min = 4, max = 120, message = "Quiz title must contain between 4 and 120 characters.")
        String title,
        @NotNull(message = "Quiz mode is required.")
        QuizMode mode,
        Integer randomCount,
        @NotNull(message = "Question order is required.")
        QuizOrderMode questionOrder,
        @NotNull(message = "Answer order is required.")
        QuizOrderMode answerOrder,
        List<Long> questionIds,
        List<Long> categoryIds
) {
}
