package pl.zieleeksw.quiz_me.question.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "questions")
class QuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "current_version_number", nullable = false)
    private int currentVersionNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QuestionEntity() {
    }

    static QuestionEntity from(
            final Question question
    ) {
        final QuestionEntity entity = new QuestionEntity();
        entity.setId(question.getId());
        entity.setCourseId(question.getCourseId());
        entity.setCurrentVersionNumber(question.getCurrentVersionNumber());
        entity.setCreatedAt(question.getCreatedAt());
        entity.setUpdatedAt(question.getUpdatedAt());
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
