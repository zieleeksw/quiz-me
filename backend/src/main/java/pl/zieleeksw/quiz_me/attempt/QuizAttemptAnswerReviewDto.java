package pl.zieleeksw.quiz_me.attempt;

public record QuizAttemptAnswerReviewDto(
        Long id,
        int displayOrder,
        String content,
        boolean correct
) {
}
