package pl.zieleeksw.quiz_me.quiz.domain;

import jakarta.persistence.*;
import pl.zieleeksw.quiz_me.quiz.QuizMode;
import pl.zieleeksw.quiz_me.quiz.QuizOrderMode;

import java.time.Instant;

@Entity
@Table(
        name = "quiz_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_quiz_versions_quiz_id_version_number", columnNames = {"quiz_id", "version_number"})
)
class QuizVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuizMode mode;

    @Column(name = "random_count")
    private Integer randomCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_order", nullable = false, length = 16)
    private QuizOrderMode questionOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_order", nullable = false, length = 16)
    private QuizOrderMode answerOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuizVersionEntity() {
    }

    static QuizVersionEntity from(
            final QuizVersion version
    ) {
        final QuizVersionEntity entity = new QuizVersionEntity();
        entity.setId(version.getId());
        entity.setQuizId(version.getQuizId());
        entity.setVersionNumber(version.getVersionNumber());
        entity.setTitle(version.getTitle());
        entity.setMode(version.getMode());
        entity.setRandomCount(version.getRandomCount());
        entity.setQuestionOrder(version.getQuestionOrder());
        entity.setAnswerOrder(version.getAnswerOrder());
        entity.setCreatedAt(version.getCreatedAt());
        return entity;
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
    }

    Long getQuizId() {
        return quizId;
    }

    void setQuizId(final Long quizId) {
        this.quizId = quizId;
    }

    int getVersionNumber() {
        return versionNumber;
    }

    void setVersionNumber(final int versionNumber) {
        this.versionNumber = versionNumber;
    }

    String getTitle() {
        return title;
    }

    void setTitle(final String title) {
        this.title = title;
    }

    QuizMode getMode() {
        return mode;
    }

    void setMode(final QuizMode mode) {
        this.mode = mode;
    }

    Integer getRandomCount() {
        return randomCount;
    }

    void setRandomCount(final Integer randomCount) {
        this.randomCount = randomCount;
    }

    QuizOrderMode getQuestionOrder() {
        return questionOrder;
    }

    void setQuestionOrder(final QuizOrderMode questionOrder) {
        this.questionOrder = questionOrder;
    }

    QuizOrderMode getAnswerOrder() {
        return answerOrder;
    }

    void setAnswerOrder(final QuizOrderMode answerOrder) {
        this.answerOrder = answerOrder;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
