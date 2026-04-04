package pl.zieleeksw.quiz_me.quiz.domain;

import java.time.Instant;

class Quiz {

    private Long id;

    private Long courseId;

    private int currentVersionNumber;

    private Instant createdAt;

    private Instant updatedAt;

    static Quiz create(
            final Long courseId,
            final Instant createdAt
    ) {
        final Quiz quiz = new Quiz();
        quiz.setCourseId(courseId);
        quiz.setCurrentVersionNumber(1);
        quiz.setCreatedAt(createdAt);
        quiz.setUpdatedAt(createdAt);
        return quiz;
    }

    static Quiz restore(
            final Long id,
            final Long courseId,
            final int currentVersionNumber,
            final Instant createdAt,
            final Instant updatedAt
    ) {
        final Quiz quiz = new Quiz();
        quiz.setId(id);
        quiz.setCourseId(courseId);
        quiz.setCurrentVersionNumber(currentVersionNumber);
        quiz.setCreatedAt(createdAt);
        quiz.setUpdatedAt(updatedAt);
        return quiz;
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
