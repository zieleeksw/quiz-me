package pl.zieleeksw.quiz_me.question.domain;

class QuestionPromptValidator {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 1000;

    void validate(
            final String prompt
    ) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Question prompt cannot be empty.");
        }

        if (prompt.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Question prompt is too short. Min length is 12 characters.");
        }

        if (prompt.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Question prompt is too long. Max length is 1000 characters.");
        }
    }
}
