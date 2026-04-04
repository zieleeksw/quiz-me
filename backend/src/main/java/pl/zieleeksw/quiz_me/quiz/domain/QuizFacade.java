package pl.zieleeksw.quiz_me.quiz.domain;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import pl.zieleeksw.quiz_me.category.domain.CategoryFacade;
import pl.zieleeksw.quiz_me.course.CourseDto;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.QuestionDto;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;
import pl.zieleeksw.quiz_me.quiz.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QuizFacade {

    private final QuizRepository quizRepository;
    private final QuizVersionRepository quizVersionRepository;
    private final QuizVersionQuestionRepository quizVersionQuestionRepository;
    private final QuizVersionCategoryRepository quizVersionCategoryRepository;
    private final CourseFacade courseFacade;
    private final CategoryFacade categoryFacade;
    private final QuestionFacade questionFacade;

    QuizFacade(
            final QuizRepository quizRepository,
            final QuizVersionRepository quizVersionRepository,
            final QuizVersionQuestionRepository quizVersionQuestionRepository,
            final QuizVersionCategoryRepository quizVersionCategoryRepository,
            final CourseFacade courseFacade,
            final CategoryFacade categoryFacade,
            final QuestionFacade questionFacade
    ) {
        this.quizRepository = quizRepository;
        this.quizVersionRepository = quizVersionRepository;
        this.quizVersionQuestionRepository = quizVersionQuestionRepository;
        this.quizVersionCategoryRepository = quizVersionCategoryRepository;
        this.courseFacade = courseFacade;
        this.categoryFacade = categoryFacade;
        this.questionFacade = questionFacade;
    }

    @Transactional
    public QuizDto createQuiz(
            final Long courseId,
            final String title,
            final QuizMode mode,
            final Integer randomCount,
            final QuizOrderMode questionOrder,
            final QuizOrderMode answerOrder,
            final List<Long> questionIds,
            final List<Long> categoryIds,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final QuizDraft draft = validateAndCreateDraft(courseId, title, mode, randomCount, questionOrder, answerOrder, questionIds, categoryIds);

        final Instant now = roundToDatabasePrecision(Instant.now());
        final Quiz quiz = Quiz.create(courseId, now);
        final QuizEntity savedQuiz = quizRepository.save(QuizEntity.from(quiz));
        final QuizVersion version = QuizVersion.create(
                savedQuiz.getId(),
                1,
                draft.title(),
                draft.mode(),
                draft.randomCount(),
                draft.questionOrder(),
                draft.answerOrder(),
                now,
                draft.questionIds(),
                draft.categoryIds()
        );

        saveVersion(version);
        return toCurrentQuizDto(savedQuiz);
    }

    public List<QuizDto> fetchQuizzes(
            final Long courseId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);

        return quizRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toCurrentQuizDto)
                .toList();
    }

    public List<QuizVersionDto> fetchQuizVersions(
            final Long courseId,
            final Long quizId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);
        final QuizEntity quiz = findQuizInCourse(quizId, courseId);

        return quizVersionRepository.findAllByQuizIdOrderByVersionNumberDesc(quiz.getId())
                .stream()
                .map(version -> toQuizVersionDto(quiz.getCourseId(), version))
                .toList();
    }

    @Transactional
    public QuizDto updateQuiz(
            final Long courseId,
            final Long quizId,
            final String title,
            final QuizMode mode,
            final Integer randomCount,
            final QuizOrderMode questionOrder,
            final QuizOrderMode answerOrder,
            final List<Long> questionIds,
            final List<Long> categoryIds,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final QuizEntity entity = findQuizInCourse(quizId, courseId);
        final QuizDraft draft = validateAndCreateDraft(courseId, title, mode, randomCount, questionOrder, answerOrder, questionIds, categoryIds);
        final QuizVersionEntity currentVersion = quizVersionRepository.findByQuizIdAndVersionNumber(entity.getId(), entity.getCurrentVersionNumber())
                .orElseThrow(() -> new IllegalStateException("Current quiz version was not found."));
        assertMeaningfulUpdate(currentVersion, draft);

        final Instant now = roundToDatabasePrecision(Instant.now());
        final Quiz quiz = Quiz.restore(
                entity.getId(),
                entity.getCourseId(),
                entity.getCurrentVersionNumber(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        final int nextVersionNumber = quiz.advanceVersion(now);

        final QuizEntity savedQuiz = quizRepository.save(QuizEntity.from(quiz));
        final QuizVersion nextVersion = QuizVersion.create(
                savedQuiz.getId(),
                nextVersionNumber,
                draft.title(),
                draft.mode(),
                draft.randomCount(),
                draft.questionOrder(),
                draft.answerOrder(),
                now,
                draft.questionIds(),
                draft.categoryIds()
        );

        saveVersion(nextVersion);
        return toCurrentQuizDto(savedQuiz);
    }

    @Transactional
    public void deleteQuiz(
            final Long courseId,
            final Long quizId,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final QuizEntity quiz = findQuizInCourse(quizId, courseId);
        quizRepository.delete(quiz);
    }

    private QuizDraft validateAndCreateDraft(
            final Long courseId,
            final String title,
            final QuizMode mode,
            final Integer randomCount,
            final QuizOrderMode questionOrder,
            final QuizOrderMode answerOrder,
            final List<Long> questionIds,
            final List<Long> categoryIds
    ) {
        final String normalizedTitle = normalize(title);
        final List<Long> normalizedQuestionIds = normalizeIds(questionIds);
        final List<Long> normalizedCategoryIds = normalizeIds(categoryIds);

        validateTitle(normalizedTitle);

        if (mode == null) {
            throw new IllegalArgumentException("Quiz mode is required.");
        }

        if (questionOrder == null) {
            throw new IllegalArgumentException("Question order is required.");
        }

        if (answerOrder == null) {
            throw new IllegalArgumentException("Answer order is required.");
        }

        if (mode == QuizMode.MANUAL) {
            if (normalizedQuestionIds.isEmpty()) {
                throw new IllegalArgumentException("Manual quiz must contain at least 1 question.");
            }

            if (randomCount != null) {
                throw new IllegalArgumentException("Manual quiz cannot define a random question count.");
            }

            if (!normalizedCategoryIds.isEmpty()) {
                throw new IllegalArgumentException("Manual quiz cannot define random category filters.");
            }

            assertQuestionsBelongToCourse(courseId, normalizedQuestionIds);
        } else {
            if (randomCount == null || randomCount < 1) {
                throw new IllegalArgumentException("Random quiz must define a random count greater than 0.");
            }

            if (!normalizedQuestionIds.isEmpty()) {
                throw new IllegalArgumentException("Random quiz cannot contain manually selected questions.");
            }

            if (!normalizedCategoryIds.isEmpty()) {
                categoryFacade.findActiveCategoriesByIdsOrThrow(courseId, normalizedCategoryIds);
                assertRandomCategoriesMatchAtLeastOneQuestion(courseId, normalizedCategoryIds);
            }
        }

        return new QuizDraft(
                normalizedTitle,
                mode,
                mode == QuizMode.RANDOM ? randomCount : null,
                questionOrder,
                answerOrder,
                normalizedQuestionIds,
                normalizedCategoryIds
        );
    }

    private void validateTitle(
            final String title
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Quiz title is required.");
        }

        if (title.length() < 4 || title.length() > 120) {
            throw new IllegalArgumentException("Quiz title must contain between 4 and 120 characters.");
        }
    }

    private void assertQuestionsBelongToCourse(
            final Long courseId,
            final List<Long> questionIds
    ) {
        final Set<Long> availableQuestionIds = questionFacade.fetchQuestions(courseId)
                .stream()
                .map(QuestionDto::id)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (!availableQuestionIds.containsAll(questionIds)) {
            throw new IllegalArgumentException("Some selected questions are invalid for this course.");
        }
    }

    private void assertRandomCategoriesMatchAtLeastOneQuestion(
            final Long courseId,
            final List<Long> categoryIds
    ) {
        final boolean matches = questionFacade.fetchQuestions(courseId)
                .stream()
                .anyMatch(question -> question.categories()
                        .stream()
                        .anyMatch(category -> categoryIds.contains(category.id())));

        if (!matches) {
            throw new IllegalArgumentException("Selected random categories do not match any current course question.");
        }
    }

    private void saveVersion(
            final QuizVersion version
    ) {
        final QuizVersionEntity savedVersion = quizVersionRepository.save(QuizVersionEntity.from(version));
        final List<QuizVersionQuestionEntity> questionEntities = new ArrayList<>();
        final List<QuizVersionCategoryEntity> categoryEntities = new ArrayList<>();

        for (int index = 0; index < version.getQuestionIds().size(); index++) {
            questionEntities.add(QuizVersionQuestionEntity.from(savedVersion.getId(), version.getQuestionIds().get(index), index));
        }

        for (int index = 0; index < version.getCategoryIds().size(); index++) {
            categoryEntities.add(QuizVersionCategoryEntity.from(savedVersion.getId(), version.getCategoryIds().get(index), index));
        }

        if (!questionEntities.isEmpty()) {
            quizVersionQuestionRepository.saveAll(questionEntities);
        }

        if (!categoryEntities.isEmpty()) {
            quizVersionCategoryRepository.saveAll(categoryEntities);
        }
    }

    private QuizEntity findQuizInCourse(
            final Long quizId,
            final Long courseId
    ) {
        final QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> QuizNotFoundException.forId(quizId));

        if (!quiz.getCourseId().equals(courseId)) {
            throw QuizNotFoundException.forId(quizId);
        }

        return quiz;
    }

    private QuizDto toCurrentQuizDto(
            final QuizEntity quiz
    ) {
        final QuizVersionEntity currentVersion = quizVersionRepository.findByQuizIdAndVersionNumber(quiz.getId(), quiz.getCurrentVersionNumber())
                .orElseThrow(() -> new IllegalStateException("Current quiz version was not found."));
        final List<Long> questionIds = quizVersionQuestionRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(currentVersion.getId())
                .stream()
                .map(QuizVersionQuestionEntity::getQuestionId)
                .toList();
        final List<Long> categoryIds = currentVersion.getMode() == QuizMode.MANUAL
                ? List.of()
                : quizVersionCategoryRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(currentVersion.getId())
                        .stream()
                        .map(QuizVersionCategoryEntity::getCategoryId)
                        .toList();

        return new QuizDto(
                quiz.getId(),
                quiz.getCourseId(),
                quiz.getCurrentVersionNumber(),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt(),
                currentVersion.getTitle(),
                currentVersion.getMode(),
                currentVersion.getRandomCount(),
                currentVersion.getQuestionOrder(),
                currentVersion.getAnswerOrder(),
                questionIds,
                categoryFacade.findActiveCategoriesByIdsInCourse(quiz.getCourseId(), categoryIds)
                        .stream()
                        .map(category -> new QuizCategoryDto(category.id(), category.name()))
                        .toList()
        );
    }

    private QuizVersionDto toQuizVersionDto(
            final Long courseId,
            final QuizVersionEntity version
    ) {
        final List<Long> questionIds = quizVersionQuestionRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(version.getId())
                .stream()
                .map(QuizVersionQuestionEntity::getQuestionId)
                .toList();
        final List<Long> categoryIds = version.getMode() == QuizMode.MANUAL
                ? List.of()
                : quizVersionCategoryRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(version.getId())
                        .stream()
                        .map(QuizVersionCategoryEntity::getCategoryId)
                        .toList();

        return new QuizVersionDto(
                version.getId(),
                version.getQuizId(),
                version.getVersionNumber(),
                version.getCreatedAt(),
                version.getTitle(),
                version.getMode(),
                version.getRandomCount(),
                version.getQuestionOrder(),
                version.getAnswerOrder(),
                questionIds,
                categoryFacade.findCategoriesByIdsInCourse(courseId, categoryIds)
                        .stream()
                        .map(category -> new QuizCategoryDto(category.id(), category.name()))
                        .toList()
        );
    }

    private void assertMeaningfulUpdate(
            final QuizVersionEntity currentVersion,
            final QuizDraft draft
    ) {
        final List<Long> currentQuestionIds = quizVersionQuestionRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(currentVersion.getId())
                .stream()
                .map(QuizVersionQuestionEntity::getQuestionId)
                .toList();
        final List<Long> currentCategoryIds = quizVersionCategoryRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(currentVersion.getId())
                .stream()
                .map(QuizVersionCategoryEntity::getCategoryId)
                .toList();

        if (currentVersion.getTitle().equals(draft.title())
                && currentVersion.getMode() == draft.mode()
                && java.util.Objects.equals(currentVersion.getRandomCount(), draft.randomCount())
                && currentVersion.getQuestionOrder() == draft.questionOrder()
                && currentVersion.getAnswerOrder() == draft.answerOrder()
                && currentQuestionIds.equals(draft.questionIds())
                && currentCategoryIds.equals(draft.categoryIds())) {
            throw new IllegalArgumentException(
                    "Quiz update must change the title, mode, random settings, order settings, questions, or categories."
            );
        }
    }

    private List<Long> normalizeIds(
            final List<Long> ids
    ) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();

        for (final Long id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("Selected identifiers cannot contain empty values.");
            }

            uniqueIds.add(id);
        }

        return List.copyOf(uniqueIds);
    }

    private String normalize(
            final String value
    ) {
        return value == null ? null : value.trim();
    }

    private Instant roundToDatabasePrecision(
            final Instant instant
    ) {
        long epochSecond = instant.getEpochSecond();
        long roundedMicros = (instant.getNano() + 500L) / 1_000L;

        if (roundedMicros == 1_000_000L) {
            epochSecond++;
            roundedMicros = 0L;
        }

        return Instant.ofEpochSecond(epochSecond, roundedMicros * 1_000L);
    }

    private void assertCanManageCourse(
            final CourseDto course,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        if (isAdmin || course.ownerUserId().equals(actorUserId)) {
            return;
        }

        throw new AccessDeniedException("You cannot manage quizzes for this course.");
    }

    private record QuizDraft(
            String title,
            QuizMode mode,
            Integer randomCount,
            QuizOrderMode questionOrder,
            QuizOrderMode answerOrder,
            List<Long> questionIds,
            List<Long> categoryIds
    ) {
    }
}
