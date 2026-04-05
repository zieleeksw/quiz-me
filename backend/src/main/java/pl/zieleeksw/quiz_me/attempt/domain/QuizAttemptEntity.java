package pl.zieleeksw.quiz_me.attempt.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
class QuizAttemptEntity {

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

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    protected QuizAttemptEntity() {
    }

    static QuizAttemptEntity from(
            final QuizAttempt attempt
    ) {
        final QuizAttemptEntity entity = new QuizAttemptEntity();
        entity.setId(attempt.getId());
        entity.setCourseId(attempt.getCourseId());
        entity.setQuizId(attempt.getQuizId());
        entity.setUserId(attempt.getUserId());
        entity.setQuizTitle(attempt.getQuizTitle());
        entity.setCorrectAnswers(attempt.getCorrectAnswers());
        entity.setTotalQuestions(attempt.getTotalQuestions());
        entity.setFinishedAt(attempt.getFinishedAt());
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

    Instant getFinishedAt() {
        return finishedAt;
    }

    void setFinishedAt(final Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
