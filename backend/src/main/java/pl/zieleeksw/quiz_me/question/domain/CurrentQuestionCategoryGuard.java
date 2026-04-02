package pl.zieleeksw.quiz_me.question.domain;

import java.util.List;
import java.util.Set;

public class CurrentQuestionCategoryGuard {

    private final QuestionRepository questionRepository;

    private final QuestionVersionRepository questionVersionRepository;

    private final QuestionVersionCategoryRepository questionVersionCategoryRepository;

    CurrentQuestionCategoryGuard(
            final QuestionRepository questionRepository,
            final QuestionVersionRepository questionVersionRepository,
            final QuestionVersionCategoryRepository questionVersionCategoryRepository
    ) {
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionVersionCategoryRepository = questionVersionCategoryRepository;
    }

    public int countQuestionsThatWouldLoseAllActiveCategories(
            final Long courseId,
            final Long categoryId,
            final List<Long> activeCategoryIds
    ) {
        final Set<Long> remainingActiveCategoryIds = activeCategoryIds.stream()
                .filter(activeCategoryId -> !activeCategoryId.equals(categoryId))
                .collect(java.util.stream.Collectors.toSet());
        int blockedQuestionsCount = 0;

        for (final QuestionEntity question : questionRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId)) {
            final QuestionVersionEntity currentVersion = questionVersionRepository.findByQuestionIdAndVersionNumber(
                            question.getId(),
                            question.getCurrentVersionNumber()
                    )
                    .orElseThrow(() -> new IllegalStateException("Current question version was not found."));
            final List<Long> currentCategoryIds = questionVersionCategoryRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(currentVersion.getId())
                    .stream()
                    .map(QuestionVersionCategoryEntity::getCategoryId)
                    .toList();

            if (!currentCategoryIds.contains(categoryId)) {
                continue;
            }

            final boolean hasAnotherActiveCategory = currentCategoryIds.stream()
                    .anyMatch(currentCategoryId -> !currentCategoryId.equals(categoryId) && remainingActiveCategoryIds.contains(currentCategoryId));

            if (!hasAnotherActiveCategory) {
                blockedQuestionsCount++;
            }
        }

        return blockedQuestionsCount;
    }
}
