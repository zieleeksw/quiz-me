package pl.zieleeksw.quiz_me.category.domain;

import java.time.Instant;

class Category {

    private Long id;

    private Long courseId;

    private String name;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    static Category create(
            final Long courseId,
            final String name,
            final Instant createdAt
    ) {
        final Category category = new Category();
        category.setCourseId(courseId);
        category.setName(name);
        category.setActive(true);
        category.setCreatedAt(createdAt);
        category.setUpdatedAt(createdAt);
        return category;
    }

    static Category restore(
            final Long id,
            final Long courseId,
            final String name,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt
    ) {
        final Category category = new Category();
        category.setId(id);
        category.setCourseId(courseId);
        category.setName(name);
        category.setActive(active);
        category.setCreatedAt(createdAt);
        category.setUpdatedAt(updatedAt);
        return category;
    }

    void rename(
            final String name,
            final Instant updatedAt
    ) {
        setName(name);
        setUpdatedAt(updatedAt);
    }

    void archive(
            final Instant updatedAt
    ) {
        setActive(false);
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

    String getName() {
        return name;
    }

    void setName(final String name) {
        this.name = name;
    }

    boolean isActive() {
        return active;
    }

    void setActive(final boolean active) {
        this.active = active;
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
