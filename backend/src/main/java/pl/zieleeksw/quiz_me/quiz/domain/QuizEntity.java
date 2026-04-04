package pl.zieleeksw.quiz_me.quiz.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "quizzes")
class QuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "current_version_number", nullable = false)
    private int currentVersionNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QuizEntity() {
    }

    static QuizEntity from(
            final Quiz quiz
    ) {
        final QuizEntity entity = new QuizEntity();
        entity.setId(quiz.getId());
        entity.setCourseId(quiz.getCourseId());
        entity.setActive(quiz.isActive());
        entity.setCurrentVersionNumber(quiz.getCurrentVersionNumber());
        entity.setCreatedAt(quiz.getCreatedAt());
        entity.setUpdatedAt(quiz.getUpdatedAt());
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

    boolean isActive() {
        return active;
    }

    void setActive(final boolean active) {
        this.active = active;
    }

    int getCurrentVersionNumber() {
        return currentVersionNumber;
    }

    void setCurrentVersionNumber(final int currentVersionNumber) {
        this.currentVersionNumber = currentVersionNumber;
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
