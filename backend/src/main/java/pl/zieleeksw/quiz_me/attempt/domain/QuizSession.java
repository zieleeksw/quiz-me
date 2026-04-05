package pl.zieleeksw.quiz_me.attempt.domain;

import java.time.Instant;

class QuizSession {

    private Long id;

    private Long courseId;

    private Long quizId;

    private Long userId;

    private String quizTitle;

    private String questionIdsJson;

    private String answersJson;

    private int currentIndex;

    private Instant createdAt;

    private Instant updatedAt;

    static QuizSession create(
            final Long courseId,
            final Long quizId,
            final Long userId,
            final String quizTitle,
            final String questionIdsJson,
            final String answersJson,
            final int currentIndex,
            final Instant createdAt
    ) {
        final QuizSession session = new QuizSession();
        session.setCourseId(courseId);
        session.setQuizId(quizId);
        session.setUserId(userId);
        session.setQuizTitle(quizTitle);
        session.setQuestionIdsJson(questionIdsJson);
        session.setAnswersJson(answersJson);
        session.setCurrentIndex(currentIndex);
        session.setCreatedAt(createdAt);
        session.setUpdatedAt(createdAt);
        return session;
    }

    static QuizSession restore(
            final Long id,
            final Long courseId,
            final Long quizId,
            final Long userId,
            final String quizTitle,
            final String questionIdsJson,
            final String answersJson,
            final int currentIndex,
            final Instant createdAt,
            final Instant updatedAt
    ) {
        final QuizSession session = new QuizSession();
        session.setId(id);
        session.setCourseId(courseId);
        session.setQuizId(quizId);
        session.setUserId(userId);
        session.setQuizTitle(quizTitle);
        session.setQuestionIdsJson(questionIdsJson);
        session.setAnswersJson(answersJson);
        session.setCurrentIndex(currentIndex);
        session.setCreatedAt(createdAt);
        session.setUpdatedAt(updatedAt);
        return session;
    }

    void updateProgress(
            final String answersJson,
            final int currentIndex,
            final Instant updatedAt
    ) {
        setAnswersJson(answersJson);
        setCurrentIndex(currentIndex);
        setUpdatedAt(updatedAt);
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

    String getQuestionIdsJson() {
        return questionIdsJson;
    }

    void setQuestionIdsJson(final String questionIdsJson) {
        this.questionIdsJson = questionIdsJson;
    }

    String getAnswersJson() {
        return answersJson;
    }

    void setAnswersJson(final String answersJson) {
        this.answersJson = answersJson;
    }

    int getCurrentIndex() {
        return currentIndex;
    }

    void setCurrentIndex(final int currentIndex) {
        this.currentIndex = currentIndex;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
