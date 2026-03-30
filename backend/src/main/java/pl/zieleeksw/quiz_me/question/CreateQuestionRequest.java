package pl.zieleeksw.quiz_me.question;

import java.util.List;

public record CreateQuestionRequest(
        @ValidQuestionPrompt String prompt,
        @ValidQuestionAnswers List<QuestionAnswerRequest> answers
) {
}
