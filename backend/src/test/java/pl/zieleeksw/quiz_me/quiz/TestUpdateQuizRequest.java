package pl.zieleeksw.quiz_me.quiz;

import java.util.List;

public record TestUpdateQuizRequest(
        String title,
        String mode,
        Integer randomCount,
        String questionOrder,
        String answerOrder,
        List<Long> questionIds,
        List<Long> categoryIds
) {
}
