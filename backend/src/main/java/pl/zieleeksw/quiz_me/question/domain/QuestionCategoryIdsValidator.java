package pl.zieleeksw.quiz_me.question.domain;

import java.util.List;
import java.util.Set;

class QuestionCategoryIdsValidator {

    void validate(
            final List<Long> categoryIds
    ) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("Question must contain at least 1 category.");
        }

        if (categoryIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Question category ids are invalid.");
        }

        if (Set.copyOf(categoryIds).size() != categoryIds.size()) {
            throw new IllegalArgumentException("Question category ids cannot contain duplicates.");
        }
    }
}
