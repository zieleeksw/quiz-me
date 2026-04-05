package pl.zieleeksw.quiz_me.attempt;

import java.util.List;

public record TestSubmitQuizAttemptRequest(
        List<TestQuizAttemptAnswerRequest> answers
) {
}
