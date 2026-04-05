package pl.zieleeksw.quiz_me.attempt.domain;

public class QuizSessionNotFoundException extends RuntimeException {

    private QuizSessionNotFoundException(
            final String message
    ) {
        super(message);
    }

    public static QuizSessionNotFoundException forQuizId(
            final Long quizId
    ) {
        return new QuizSessionNotFoundException("Quiz session for quiz with id " + quizId + " was not found.");
    }
}
