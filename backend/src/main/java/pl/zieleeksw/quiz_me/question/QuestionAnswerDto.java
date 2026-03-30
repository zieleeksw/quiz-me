package pl.zieleeksw.quiz_me.question;

public record QuestionAnswerDto(
        Long id,
        int displayOrder,
        String content,
        boolean correct
) {
}
