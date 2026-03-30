package pl.zieleeksw.quiz_me.question;

public record QuestionAnswerRequest(
        String content,
        boolean correct
) {
}
