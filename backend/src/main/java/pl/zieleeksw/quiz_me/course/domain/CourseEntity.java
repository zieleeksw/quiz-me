package pl.zieleeksw.quiz_me.course.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "courses")
class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    protected CourseEntity() {
    }

    static CourseEntity from(
            final Course course
    ) {
        final CourseEntity entity = new CourseEntity();
        entity.setId(course.getId());
        entity.setName(course.getName());
        entity.setDescription(course.getDescription());
        entity.setCreatedAt(course.getCreatedAt());
        entity.setOwnerUserId(course.getOwnerUserId());
        return entity;
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
