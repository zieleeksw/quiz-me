package pl.zieleeksw.quiz_me.question.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "question_version_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_question_version_categories_question_version_id_display_order", columnNames = {"question_version_id", "display_order"})
)
class QuestionVersionCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_version_id", nullable = false)
    private Long questionVersionId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected QuestionVersionCategoryEntity() {
    }

    static QuestionVersionCategoryEntity from(
            final Long questionVersionId,
            final Long categoryId,
            final int displayOrder
    ) {
        final QuestionVersionCategoryEntity entity = new QuestionVersionCategoryEntity();
        entity.setQuestionVersionId(questionVersionId);
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

    Long getQuestionVersionId() {
        return questionVersionId;
    }

    void setQuestionVersionId(final Long questionVersionId) {
        this.questionVersionId = questionVersionId;
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
