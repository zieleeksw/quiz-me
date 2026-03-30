package pl.zieleeksw.quiz_me.question.domain;

public class QuestionNotFoundException extends RuntimeException {

    private QuestionNotFoundException(
            final String message
    ) {
        super(message);
    }

    public static QuestionNotFoundException forId(
            final Long id
    ) {
        return new QuestionNotFoundException(String.format("Question with id %s was not found.", id));
    }
}
