package pl.zieleeksw.quiz_me.quiz.domain;

public class QuizNotFoundException extends RuntimeException {

    private QuizNotFoundException(
            final String message
    ) {
        super(message);
    }

    public static QuizNotFoundException forId(
            final Long id
    ) {
        return new QuizNotFoundException("Quiz with id " + id + " was not found.");
    }
}
