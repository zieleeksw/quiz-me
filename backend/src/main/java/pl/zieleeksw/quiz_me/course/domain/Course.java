package pl.zieleeksw.quiz_me.course.domain;

import java.time.Instant;

class Course {

    private Long id;

    private String name;

    private String description;

    private Instant createdAt;

    private Long ownerUserId;

    static Course create(
            final String name,
            final String description,
            final Instant createdAt,
            final Long ownerUserId
    ) {
        final Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        course.setCreatedAt(createdAt);
        course.setOwnerUserId(ownerUserId);
        return course;
    }

    static Course restore(
            final Long id,
            final String name,
            final String description,
            final Instant createdAt,
            final Long ownerUserId
    ) {
        final Course course = create(name, description, createdAt, ownerUserId);
        course.setId(id);
        return course;
    }

    void update(
            final String name,
            final String description
    ) {
        setName(name);
        setDescription(description);
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
    }

    String getName() {
        return name;
    }

    void setName(final String name) {
        this.name = name;
    }

    String getDescription() {
        return description;
    }

    void setDescription(final String description) {
        this.description = description;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    Long getOwnerUserId() {
        return ownerUserId;
    }

    void setOwnerUserId(final Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
