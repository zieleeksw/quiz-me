package pl.zieleeksw.quiz_me.attempt.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "quiz_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quiz_sessions_course_quiz_user",
                columnNames = {"course_id", "quiz_id", "user_id"}
        )
)
class QuizSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quiz_title", nullable = false, length = 120)
    private String quizTitle;

    @Column(name = "question_ids_json", nullable = false, columnDefinition = "TEXT")
    private String questionIdsJson;

    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "current_index", nullable = false)
    private int currentIndex;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QuizSessionEntity() {
    }

    static QuizSessionEntity from(
            final QuizSession session
    ) {
        final QuizSessionEntity entity = new QuizSessionEntity();
        entity.setId(session.getId());
        entity.setCourseId(session.getCourseId());
        entity.setQuizId(session.getQuizId());
        entity.setUserId(session.getUserId());
        entity.setQuizTitle(session.getQuizTitle());
        entity.setQuestionIdsJson(session.getQuestionIdsJson());
        entity.setAnswersJson(session.getAnswersJson());
        entity.setCurrentIndex(session.getCurrentIndex());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setUpdatedAt(session.getUpdatedAt());
        return entity;
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
