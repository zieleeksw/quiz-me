package pl.zieleeksw.quiz_me.question;

public record TestQuestionAnswerRequest(
        String content,
        boolean correct
) {
}
