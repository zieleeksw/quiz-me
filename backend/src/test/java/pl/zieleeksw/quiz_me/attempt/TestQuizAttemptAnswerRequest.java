package pl.zieleeksw.quiz_me.attempt;

public record TestQuizAttemptAnswerRequest(
        Long questionId,
        Long answerId
) {
}
