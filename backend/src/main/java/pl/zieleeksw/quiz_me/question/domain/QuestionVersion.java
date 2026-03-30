package pl.zieleeksw.quiz_me.question.domain;

import java.time.Instant;
import java.util.List;

class QuestionVersion {

    private Long id;

    private Long questionId;

    private int versionNumber;

    private String prompt;

    private Instant createdAt;

    private List<QuestionAnswer> answers;

    static QuestionVersion create(
            final Long questionId,
            final int versionNumber,
            final String prompt,
            final Instant createdAt,
            final List<QuestionAnswer> answers
    ) {
        final QuestionVersion questionVersion = new QuestionVersion();
        questionVersion.setQuestionId(questionId);
        questionVersion.setVersionNumber(versionNumber);
        questionVersion.setPrompt(prompt);
        questionVersion.setCreatedAt(createdAt);
        questionVersion.setAnswers(answers);
        return questionVersion;
    }

    static QuestionVersion restore(
            final Long id,
            final Long questionId,
            final int versionNumber,
            final String prompt,
            final Instant createdAt,
            final List<QuestionAnswer> answers
    ) {
        final QuestionVersion questionVersion = create(questionId, versionNumber, prompt, createdAt, answers);
        questionVersion.setId(id);
        return questionVersion;
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

    List<QuestionAnswer> getAnswers() {
        return answers;
    }

    void setAnswers(final List<QuestionAnswer> answers) {
        this.answers = answers;
    }
}
