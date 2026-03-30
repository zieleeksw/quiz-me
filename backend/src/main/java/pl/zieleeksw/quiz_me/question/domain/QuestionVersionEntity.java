package pl.zieleeksw.quiz_me.question.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "question_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_question_versions_question_id_version_number", columnNames = {"question_id", "version_number"})
)
class QuestionVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuestionVersionEntity() {
    }

    static QuestionVersionEntity from(
            final QuestionVersion questionVersion
    ) {
        final QuestionVersionEntity entity = new QuestionVersionEntity();
        entity.setId(questionVersion.getId());
        entity.setQuestionId(questionVersion.getQuestionId());
        entity.setVersionNumber(questionVersion.getVersionNumber());
        entity.setPrompt(questionVersion.getPrompt());
        entity.setCreatedAt(questionVersion.getCreatedAt());
        return entity;
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
    }

    Long getQuestionId() {
        return questionId;
    }

    void setQuestionId(final Long questionId) {
        this.questionId = questionId;
    }

    int getVersionNumber() {
        return versionNumber;
    }

    void setVersionNumber(final int versionNumber) {
        this.versionNumber = versionNumber;
    }

    String getPrompt() {
        return prompt;
    }

    void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
