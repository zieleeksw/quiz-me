package pl.zieleeksw.quiz_me.question.domain;

class QuestionAnswer {

    private Long id;

    private int displayOrder;

    private String content;

    private boolean correct;

    static QuestionAnswer create(
            final int displayOrder,
            final String content,
            final boolean correct
    ) {
        final QuestionAnswer questionAnswer = new QuestionAnswer();
        questionAnswer.setDisplayOrder(displayOrder);
        questionAnswer.setContent(content);
        questionAnswer.setCorrect(correct);
        return questionAnswer;
    }

    static QuestionAnswer restore(
            final Long id,
            final int displayOrder,
            final String content,
            final boolean correct
    ) {
        final QuestionAnswer questionAnswer = create(displayOrder, content, correct);
        questionAnswer.setId(id);
        return questionAnswer;
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
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
