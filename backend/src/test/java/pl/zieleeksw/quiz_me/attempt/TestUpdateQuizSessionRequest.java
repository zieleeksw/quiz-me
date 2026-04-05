package pl.zieleeksw.quiz_me.attempt;

import java.util.List;

public record TestUpdateQuizSessionRequest(
        Integer currentIndex,
        List<TestQuizAttemptAnswerRequest> answers
) {
}
