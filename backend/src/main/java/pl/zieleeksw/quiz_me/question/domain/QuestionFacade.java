package pl.zieleeksw.quiz_me.question.domain;

import pl.zieleeksw.quiz_me.category.CategoryDto;
import pl.zieleeksw.quiz_me.category.domain.CategoryFacade;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import pl.zieleeksw.quiz_me.course.CourseDto;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.QuestionAnswerDto;
import pl.zieleeksw.quiz_me.question.QuestionAnswerRequest;
import pl.zieleeksw.quiz_me.question.QuestionCategoryDto;
import pl.zieleeksw.quiz_me.question.QuestionDto;
import pl.zieleeksw.quiz_me.question.QuestionVersionDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class QuestionFacade {

    private final QuestionRepository questionRepository;

    private final QuestionVersionRepository questionVersionRepository;

    private final QuestionAnswerRepository questionAnswerRepository;

    private final QuestionVersionCategoryRepository questionVersionCategoryRepository;

    private final CourseFacade courseFacade;

    private final CategoryFacade categoryFacade;

    private final QuestionPromptValidator questionPromptValidator;

    private final QuestionAnswersValidator questionAnswersValidator;

    private final QuestionCategoryIdsValidator questionCategoryIdsValidator;

    QuestionFacade(
            final QuestionRepository questionRepository,
            final QuestionVersionRepository questionVersionRepository,
            final QuestionAnswerRepository questionAnswerRepository,
            final QuestionVersionCategoryRepository questionVersionCategoryRepository,
            final CourseFacade courseFacade,
            final CategoryFacade categoryFacade,
            final QuestionPromptValidator questionPromptValidator,
            final QuestionAnswersValidator questionAnswersValidator,
            final QuestionCategoryIdsValidator questionCategoryIdsValidator
    ) {
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionAnswerRepository = questionAnswerRepository;
        this.questionVersionCategoryRepository = questionVersionCategoryRepository;
        this.courseFacade = courseFacade;
        this.categoryFacade = categoryFacade;
        this.questionPromptValidator = questionPromptValidator;
        this.questionAnswersValidator = questionAnswersValidator;
        this.questionCategoryIdsValidator = questionCategoryIdsValidator;
    }

    @Transactional
    public QuestionDto createQuestion(
            final Long courseId,
            final String prompt,
            final List<QuestionAnswerRequest> answers,
            final List<Long> categoryIds,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final String normalizedPrompt = normalize(prompt);

        validatePrompt(normalizedPrompt);
        validateAnswers(answers);
        validateCategoryIds(categoryIds);
        final List<CategoryDto> categories = categoryFacade.findActiveCategoriesByIdsOrThrow(courseId, categoryIds);
        final List<QuestionAnswer> normalizedAnswers = normalizeAnswers(answers);

        final Instant now = roundToDatabasePrecision(Instant.now());
        final Question question = Question.create(courseId, now);
        final QuestionEntity savedQuestion = questionRepository.save(QuestionEntity.from(question));
        final QuestionVersion version = QuestionVersion.create(
                savedQuestion.getId(),
                1,
                normalizedPrompt,
                now,
                normalizedAnswers
        );

        saveVersion(version, categories);
        return toCurrentQuestionDto(savedQuestion);
    }

    public List<QuestionDto> fetchQuestions(
            final Long courseId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);

        return questionRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toCurrentQuestionDto)
                .toList();
    }

    public List<QuestionVersionDto> fetchQuestionVersions(
            final Long courseId,
            final Long questionId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);
        final QuestionEntity question = findQuestionInCourse(questionId, courseId);

        return questionVersionRepository.findAllByQuestionIdOrderByVersionNumberDesc(question.getId())
                .stream()
                .map(this::toQuestionVersionDto)
                .toList();
    }

    @Transactional
    public QuestionDto updateQuestion(
            final Long courseId,
            final Long questionId,
            final String prompt,
            final List<QuestionAnswerRequest> answers,
            final List<Long> categoryIds,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final QuestionEntity entity = findQuestionInCourse(questionId, courseId);
        final String normalizedPrompt = normalize(prompt);

        validatePrompt(normalizedPrompt);
        validateAnswers(answers);
        validateCategoryIds(categoryIds);
        final List<CategoryDto> categories = categoryFacade.findActiveCategoriesByIdsOrThrow(courseId, categoryIds);
        final List<QuestionAnswer> normalizedAnswers = normalizeAnswers(answers);
        final QuestionVersionEntity currentVersion = questionVersionRepository.findByQuestionIdAndVersionNumber(
                        entity.getId(),
                        entity.getCurrentVersionNumber()
                )
                .orElseThrow(() -> new IllegalStateException("Current question version was not found."));
        assertMeaningfulUpdate(currentVersion, normalizedPrompt, normalizedAnswers, categoryIds);

        final Instant now = roundToDatabasePrecision(Instant.now());
        final Question question = Question.restore(
                entity.getId(),
                entity.getCourseId(),
                entity.getCurrentVersionNumber(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        final int nextVersionNumber = question.advanceVersion(now);

        final QuestionEntity savedQuestion = questionRepository.save(QuestionEntity.from(question));
        final QuestionVersion nextVersion = QuestionVersion.create(
                savedQuestion.getId(),
                nextVersionNumber,
                normalizedPrompt,
                now,
                normalizedAnswers
        );

        saveVersion(nextVersion, categories);
        return toCurrentQuestionDto(savedQuestion);
    }

    public void validatePrompt(
            final String prompt
    ) {
        questionPromptValidator.validate(prompt);
    }

    public void validateAnswers(
            final List<QuestionAnswerRequest> answers
    ) {
        questionAnswersValidator.validate(answers);
    }

    public void validateCategoryIds(
            final List<Long> categoryIds
    ) {
        questionCategoryIdsValidator.validate(categoryIds);
    }

    private void saveVersion(
            final QuestionVersion version,
            final List<CategoryDto> categories
    ) {
        final QuestionVersionEntity savedVersion = questionVersionRepository.save(QuestionVersionEntity.from(version));
        final List<QuestionAnswerEntity> answerEntities = version.getAnswers()
                .stream()
                .map(answer -> QuestionAnswerEntity.from(savedVersion.getId(), answer))
                .toList();
        final List<QuestionVersionCategoryEntity> categoryEntities = new ArrayList<>();

        for (int index = 0; index < categories.size(); index++) {
            categoryEntities.add(QuestionVersionCategoryEntity.from(savedVersion.getId(), categories.get(index).id(), index));
        }

        questionAnswerRepository.saveAll(answerEntities);
        questionVersionCategoryRepository.saveAll(categoryEntities);
    }

    private QuestionEntity findQuestionInCourse(
            final Long questionId,
            final Long courseId
    ) {
        final QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> QuestionNotFoundException.forId(questionId));

        if (!question.getCourseId().equals(courseId)) {
            throw QuestionNotFoundException.forId(questionId);
        }

        return question;
    }

    private QuestionDto toCurrentQuestionDto(
            final QuestionEntity question
    ) {
        final QuestionVersionEntity currentVersion = questionVersionRepository.findByQuestionIdAndVersionNumber(
                        question.getId(),
                        question.getCurrentVersionNumber()
                )
                .orElseThrow(() -> new IllegalStateException("Current question version was not found."));
        final List<QuestionCategoryDto> categories = findCurrentCategoryDtos(question.getCourseId(), currentVersion.getId());
        final List<QuestionAnswerDto> answers = findAnswerDtos(currentVersion.getId());

        return new QuestionDto(
                question.getId(),
                question.getCourseId(),
                question.getCurrentVersionNumber(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                currentVersion.getPrompt(),
                categories,
                answers
        );
    }

    private QuestionVersionDto toQuestionVersionDto(
            final QuestionVersionEntity version
    ) {
        final QuestionEntity question = questionRepository.findById(version.getQuestionId())
                .orElseThrow(() -> new IllegalStateException("Question for version was not found."));

        return new QuestionVersionDto(
                version.getId(),
                version.getQuestionId(),
                version.getVersionNumber(),
                version.getCreatedAt(),
                version.getPrompt(),
                findCategoryDtos(question.getCourseId(), version.getId()),
                findAnswerDtos(version.getId())
        );
    }

    private List<QuestionCategoryDto> findCategoryDtos(
            final Long courseId,
            final Long questionVersionId
    ) {
        final List<Long> categoryIds = questionVersionCategoryRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(questionVersionId)
                .stream()
                .map(QuestionVersionCategoryEntity::getCategoryId)
                .toList();

        return categoryFacade.findCategoriesByIdsInCourse(courseId, categoryIds)
                .stream()
                .map(category -> new QuestionCategoryDto(
                        category.id(),
                        category.name()
                ))
                .toList();
    }

    private List<QuestionCategoryDto> findCurrentCategoryDtos(
            final Long courseId,
            final Long questionVersionId
    ) {
        final List<Long> categoryIds = questionVersionCategoryRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(questionVersionId)
                .stream()
                .map(QuestionVersionCategoryEntity::getCategoryId)
                .toList();

        return categoryFacade.findActiveCategoriesByIdsInCourse(courseId, categoryIds)
                .stream()
                .map(category -> new QuestionCategoryDto(
                        category.id(),
                        category.name()
                ))
                .toList();
    }

    private List<QuestionAnswerDto> findAnswerDtos(
            final Long questionVersionId
    ) {
        return questionAnswerRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(questionVersionId)
                .stream()
                .map(answer -> new QuestionAnswerDto(
                        answer.getId(),
                        answer.getDisplayOrder(),
                        answer.getContent(),
                        answer.isCorrect()
                ))
                .toList();
    }

    private List<QuestionAnswer> normalizeAnswers(
            final List<QuestionAnswerRequest> answers
    ) {
        final List<QuestionAnswer> normalizedAnswers = new ArrayList<>();

        for (int index = 0; index < answers.size(); index++) {
            final QuestionAnswerRequest answer = answers.get(index);
            normalizedAnswers.add(QuestionAnswer.create(
                    index,
                    normalize(answer.content()),
                    answer.correct()
            ));
        }

        return normalizedAnswers;
    }

    private void assertMeaningfulUpdate(
            final QuestionVersionEntity currentVersion,
            final String normalizedPrompt,
            final List<QuestionAnswer> normalizedAnswers,
            final List<Long> categoryIds
    ) {
        final List<QuestionAnswer> currentAnswers = questionAnswerRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(currentVersion.getId())
                .stream()
                .map(answer -> QuestionAnswer.restore(
                        answer.getId(),
                        answer.getDisplayOrder(),
                        answer.getContent(),
                        answer.isCorrect()
                ))
                .toList();
        final List<Long> currentCategoryIds = questionVersionCategoryRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(currentVersion.getId())
                .stream()
                .map(QuestionVersionCategoryEntity::getCategoryId)
                .toList();

        final boolean promptChanged = !currentVersion.getPrompt().equals(normalizedPrompt);
        final boolean answersChanged = !areSameAnswers(currentAnswers, normalizedAnswers);
        final boolean categoriesChanged = !currentCategoryIds.equals(categoryIds);

        if (!promptChanged && !answersChanged && !categoriesChanged) {
            throw new IllegalArgumentException("Question update must change the prompt, answers, or categories.");
        }
    }

    private boolean areSameAnswers(
            final List<QuestionAnswer> left,
            final List<QuestionAnswer> right
    ) {
        if (left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index++) {
            final QuestionAnswer leftAnswer = left.get(index);
            final QuestionAnswer rightAnswer = right.get(index);

            if (leftAnswer.getDisplayOrder() != rightAnswer.getDisplayOrder()) {
                return false;
            }

            if (!leftAnswer.getContent().equals(rightAnswer.getContent())) {
                return false;
            }

            if (leftAnswer.isCorrect() != rightAnswer.isCorrect()) {
                return false;
            }
        }

        return true;
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

        throw new AccessDeniedException("You cannot manage questions for this course.");
    }
}
