package pl.zieleeksw.quiz_me.question;

import java.util.List;

public record TestCreateQuestionRequest(
        String prompt,
        List<TestQuestionAnswerRequest> answers
) {
}
