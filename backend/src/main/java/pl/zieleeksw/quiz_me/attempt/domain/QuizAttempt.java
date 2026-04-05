package pl.zieleeksw.quiz_me.attempt.domain;

import java.time.Instant;

class QuizAttempt {

    private Long id;

    private Long courseId;

    private Long quizId;

    private Long userId;

    private String quizTitle;

    private int correctAnswers;

    private int totalQuestions;

    private String reviewSnapshotJson;

    private Instant finishedAt;

    static QuizAttempt create(
            final Long courseId,
            final Long quizId,
            final Long userId,
            final String quizTitle,
            final int correctAnswers,
            final int totalQuestions,
            final String reviewSnapshotJson,
            final Instant finishedAt
    ) {
        final QuizAttempt attempt = new QuizAttempt();
        attempt.setCourseId(courseId);
        attempt.setQuizId(quizId);
        attempt.setUserId(userId);
        attempt.setQuizTitle(quizTitle);
        attempt.setCorrectAnswers(correctAnswers);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setReviewSnapshotJson(reviewSnapshotJson);
        attempt.setFinishedAt(finishedAt);
        return attempt;
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
    }

    Long getCourseId() {
        return courseId;
    }

    void setCourseId(final Long courseId) {
        this.courseId = courseId;
    }

    Long getQuizId() {
        return quizId;
    }

    void setQuizId(final Long quizId) {
        this.quizId = quizId;
    }

    Long getUserId() {
        return userId;
    }

    void setUserId(final Long userId) {
        this.userId = userId;
    }

    String getQuizTitle() {
        return quizTitle;
    }

    void setQuizTitle(final String quizTitle) {
        this.quizTitle = quizTitle;
    }

    int getCorrectAnswers() {
        return correctAnswers;
    }

    void setCorrectAnswers(final int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    int getTotalQuestions() {
        return totalQuestions;
    }

    void setTotalQuestions(final int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    String getReviewSnapshotJson() {
        return reviewSnapshotJson;
    }

    void setReviewSnapshotJson(final String reviewSnapshotJson) {
        this.reviewSnapshotJson = reviewSnapshotJson;
    }

    Instant getFinishedAt() {
        return finishedAt;
    }

    void setFinishedAt(final Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
