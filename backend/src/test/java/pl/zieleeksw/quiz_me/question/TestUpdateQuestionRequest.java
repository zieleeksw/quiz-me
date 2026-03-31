package pl.zieleeksw.quiz_me.question;

import java.util.List;

public record TestUpdateQuestionRequest(
        String prompt,
        List<TestQuestionAnswerRequest> answers,
        List<Long> categoryIds
) {
}
