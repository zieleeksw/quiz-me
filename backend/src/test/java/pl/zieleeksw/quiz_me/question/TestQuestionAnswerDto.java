package pl.zieleeksw.quiz_me.question;

public record TestQuestionAnswerDto(
        Long id,
        int displayOrder,
        String content,
        boolean correct
) {
}
