package pl.zieleeksw.quiz_me.question.domain;

import pl.zieleeksw.quiz_me.question.QuestionAnswerRequest;

import java.util.List;

class QuestionAnswersValidator {

    private static final int MIN_ANSWERS = 2;
    private static final int MAX_ANSWERS = 6;
    private static final int MAX_CONTENT_LENGTH = 300;

    void validate(
            final List<QuestionAnswerRequest> answers
    ) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Question must contain at least 2 answers.");
        }

        if (answers.size() < MIN_ANSWERS) {
            throw new IllegalArgumentException("Question must contain at least 2 answers.");
        }

        if (answers.size() > MAX_ANSWERS) {
            throw new IllegalArgumentException("Question must contain at most 6 answers.");
        }

        final long correctAnswers = answers.stream()
                .filter(answer -> answer != null && answer.correct())
                .count();

        if (correctAnswers != 1) {
            throw new IllegalArgumentException("Question must contain exactly 1 correct answer.");
        }

        for (final QuestionAnswerRequest answer : answers) {
            if (answer == null || answer.content() == null || answer.content().isBlank()) {
                throw new IllegalArgumentException("Question answer content cannot be empty.");
            }

            if (answer.content().trim().length() > MAX_CONTENT_LENGTH) {
                throw new IllegalArgumentException("Question answer content is too long. Max length is 300 characters.");
            }
        }
    }
}
