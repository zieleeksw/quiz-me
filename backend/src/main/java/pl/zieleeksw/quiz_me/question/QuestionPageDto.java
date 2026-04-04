package pl.zieleeksw.quiz_me.question;

import java.util.List;

public record QuestionPageDto(
        List<QuestionDto> items,
        int pageNumber,
        int pageSize,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
