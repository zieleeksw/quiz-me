package pl.zieleeksw.quiz_me.quiz.domain;

import pl.zieleeksw.quiz_me.quiz.QuizMode;
import pl.zieleeksw.quiz_me.quiz.QuizOrderMode;

import java.time.Instant;
import java.util.List;

class QuizVersion {

    private Long id;

    private Long quizId;

    private int versionNumber;

    private String title;

    private QuizMode mode;

    private Integer randomCount;

    private QuizOrderMode questionOrder;

    private QuizOrderMode answerOrder;

    private Instant createdAt;

    private List<Long> questionIds;

    private List<Long> categoryIds;

    static QuizVersion create(
            final Long quizId,
            final int versionNumber,
            final String title,
            final QuizMode mode,
            final Integer randomCount,
            final QuizOrderMode questionOrder,
            final QuizOrderMode answerOrder,
            final Instant createdAt,
            final List<Long> questionIds,
            final List<Long> categoryIds
    ) {
        final QuizVersion version = new QuizVersion();
        version.setQuizId(quizId);
        version.setVersionNumber(versionNumber);
        version.setTitle(title);
        version.setMode(mode);
        version.setRandomCount(randomCount);
        version.setQuestionOrder(questionOrder);
        version.setAnswerOrder(answerOrder);
        version.setCreatedAt(createdAt);
        version.setQuestionIds(questionIds);
        version.setCategoryIds(categoryIds);
        return version;
    }

    static QuizVersion restore(
            final Long id,
            final Long quizId,
            final int versionNumber,
            final String title,
            final QuizMode mode,
            final Integer randomCount,
            final QuizOrderMode questionOrder,
            final QuizOrderMode answerOrder,
            final Instant createdAt,
            final List<Long> questionIds,
            final List<Long> categoryIds
    ) {
        final QuizVersion version = create(
                quizId,
                versionNumber,
                title,
                mode,
                randomCount,
                questionOrder,
                answerOrder,
                createdAt,
                questionIds,
                categoryIds
        );
        version.setId(id);
        return version;
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

    List<Long> getQuestionIds() {
        return questionIds;
    }

    void setQuestionIds(final List<Long> questionIds) {
        this.questionIds = questionIds;
    }

    List<Long> getCategoryIds() {
        return categoryIds;
    }

    void setCategoryIds(final List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
