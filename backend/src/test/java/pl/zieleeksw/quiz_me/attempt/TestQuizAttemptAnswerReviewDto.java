package pl.zieleeksw.quiz_me.attempt;

public record TestQuizAttemptAnswerReviewDto(
        Long id,
        int displayOrder,
        String content,
        boolean correct
) {
}
