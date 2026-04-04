package pl.zieleeksw.quiz_me.quiz.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "quiz_version_questions",
        uniqueConstraints = @UniqueConstraint(name = "uq_quiz_version_questions_quiz_version_id_display_order", columnNames = {"quiz_version_id", "display_order"})
)
class QuizVersionQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_version_id", nullable = false)
    private Long quizVersionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected QuizVersionQuestionEntity() {
    }

    static QuizVersionQuestionEntity from(
            final Long quizVersionId,
            final Long questionId,
            final int displayOrder
    ) {
        final QuizVersionQuestionEntity entity = new QuizVersionQuestionEntity();
        entity.setQuizVersionId(quizVersionId);
        entity.setQuestionId(questionId);
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

    Long getQuestionId() {
        return questionId;
    }

    void setQuestionId(final Long questionId) {
        this.questionId = questionId;
    }

    int getDisplayOrder() {
        return displayOrder;
    }

    void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
