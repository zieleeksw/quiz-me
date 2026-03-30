package pl.zieleeksw.quiz_me.question.domain;

import java.time.Instant;

class Question {

    private Long id;

    private Long courseId;

    private int currentVersionNumber;

    private Instant createdAt;

    private Instant updatedAt;

    static Question create(
            final Long courseId,
            final Instant createdAt
    ) {
        final Question question = new Question();
        question.setCourseId(courseId);
        question.setCurrentVersionNumber(1);
        question.setCreatedAt(createdAt);
        question.setUpdatedAt(createdAt);
        return question;
    }

    static Question restore(
            final Long id,
            final Long courseId,
            final int currentVersionNumber,
            final Instant createdAt,
            final Instant updatedAt
    ) {
        final Question question = new Question();
        question.setId(id);
        question.setCourseId(courseId);
        question.setCurrentVersionNumber(currentVersionNumber);
        question.setCreatedAt(createdAt);
        question.setUpdatedAt(updatedAt);
        return question;
    }

    int advanceVersion(
            final Instant updatedAt
    ) {
        setCurrentVersionNumber(currentVersionNumber + 1);
        setUpdatedAt(updatedAt);
        return currentVersionNumber;
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
