package pl.zieleeksw.quiz_me.quiz.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "quiz_version_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_quiz_version_categories_quiz_version_id_display_order", columnNames = {"quiz_version_id", "display_order"})
)
class QuizVersionCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_version_id", nullable = false)
    private Long quizVersionId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected QuizVersionCategoryEntity() {
    }

    static QuizVersionCategoryEntity from(
            final Long quizVersionId,
            final Long categoryId,
            final int displayOrder
    ) {
        final QuizVersionCategoryEntity entity = new QuizVersionCategoryEntity();
        entity.setQuizVersionId(quizVersionId);
        entity.setCategoryId(categoryId);
        entity.setDisplayOrder(displayOrder);
        return entity;
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
    }

    Long getQuizVersionId() {
        return quizVersionId;
    }

    void setQuizVersionId(final Long quizVersionId) {
        this.quizVersionId = quizVersionId;
    }

    Long getCategoryId() {
        return categoryId;
    }

    void setCategoryId(final Long categoryId) {
        this.categoryId = categoryId;
    }

    int getDisplayOrder() {
        return displayOrder;
    }

    void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
