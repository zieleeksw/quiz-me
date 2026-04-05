package pl.zieleeksw.quiz_me.attempt.domain;

public class QuizAttemptNotFoundException extends RuntimeException {

    private QuizAttemptNotFoundException(
            final String message
    ) {
        super(message);
    }

    public static QuizAttemptNotFoundException forId(
            final Long attemptId
    ) {
        return new QuizAttemptNotFoundException("Quiz attempt with id " + attemptId + " was not found.");
    }
}
