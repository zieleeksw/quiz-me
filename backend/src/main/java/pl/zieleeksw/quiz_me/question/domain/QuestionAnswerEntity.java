package pl.zieleeksw.quiz_me.question.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "question_answers",
        uniqueConstraints = @UniqueConstraint(name = "uq_question_answers_question_version_id_display_order", columnNames = {"question_version_id", "display_order"})
)
class QuestionAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_version_id", nullable = false)
    private Long questionVersionId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(nullable = false)
    private boolean correct;

    protected QuestionAnswerEntity() {
    }

    static QuestionAnswerEntity from(
            final Long questionVersionId,
            final QuestionAnswer questionAnswer
    ) {
        final QuestionAnswerEntity entity = new QuestionAnswerEntity();
        entity.setId(questionAnswer.getId());
        entity.setQuestionVersionId(questionVersionId);
        entity.setDisplayOrder(questionAnswer.getDisplayOrder());
        entity.setContent(questionAnswer.getContent());
        entity.setCorrect(questionAnswer.isCorrect());
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

    int getDisplayOrder() {
        return displayOrder;
    }

    void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    String getContent() {
        return content;
    }

    void setContent(final String content) {
        this.content = content;
    }

    boolean isCorrect() {
        return correct;
    }

    void setCorrect(final boolean correct) {
        this.correct = correct;
    }
}
