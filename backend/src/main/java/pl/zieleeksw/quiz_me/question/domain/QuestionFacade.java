package pl.zieleeksw.quiz_me.question.domain;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import pl.zieleeksw.quiz_me.course.CourseDto;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.QuestionAnswerDto;
import pl.zieleeksw.quiz_me.question.QuestionAnswerRequest;
import pl.zieleeksw.quiz_me.question.QuestionDto;
import pl.zieleeksw.quiz_me.question.QuestionVersionDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class QuestionFacade {

    private final QuestionRepository questionRepository;

    private final QuestionVersionRepository questionVersionRepository;

    private final QuestionAnswerRepository questionAnswerRepository;

    private final CourseFacade courseFacade;

    private final QuestionPromptValidator questionPromptValidator;

    private final QuestionAnswersValidator questionAnswersValidator;

    QuestionFacade(
            final QuestionRepository questionRepository,
            final QuestionVersionRepository questionVersionRepository,
            final QuestionAnswerRepository questionAnswerRepository,
            final CourseFacade courseFacade,
            final QuestionPromptValidator questionPromptValidator,
            final QuestionAnswersValidator questionAnswersValidator
    ) {
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionAnswerRepository = questionAnswerRepository;
        this.courseFacade = courseFacade;
        this.questionPromptValidator = questionPromptValidator;
        this.questionAnswersValidator = questionAnswersValidator;
    }

    @Transactional
    public QuestionDto createQuestion(
            final Long courseId,
            final String prompt,
            final List<QuestionAnswerRequest> answers,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final String normalizedPrompt = normalize(prompt);

        validatePrompt(normalizedPrompt);
        validateAnswers(answers);
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

        saveVersion(version);
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
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final QuestionEntity entity = findQuestionInCourse(questionId, courseId);
        final String normalizedPrompt = normalize(prompt);

        validatePrompt(normalizedPrompt);
        validateAnswers(answers);
        final List<QuestionAnswer> normalizedAnswers = normalizeAnswers(answers);

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

        saveVersion(nextVersion);
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

    private void saveVersion(
            final QuestionVersion version
    ) {
        final QuestionVersionEntity savedVersion = questionVersionRepository.save(QuestionVersionEntity.from(version));
        final List<QuestionAnswerEntity> answerEntities = version.getAnswers()
                .stream()
                .map(answer -> QuestionAnswerEntity.from(savedVersion.getId(), answer))
                .toList();

        questionAnswerRepository.saveAll(answerEntities);
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
        final List<QuestionAnswerDto> answers = findAnswerDtos(currentVersion.getId());

        return new QuestionDto(
                question.getId(),
                question.getCourseId(),
                question.getCurrentVersionNumber(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                currentVersion.getPrompt(),
                answers
        );
    }

    private QuestionVersionDto toQuestionVersionDto(
            final QuestionVersionEntity version
    ) {
        return new QuestionVersionDto(
                version.getId(),
                version.getQuestionId(),
                version.getVersionNumber(),
                version.getCreatedAt(),
                version.getPrompt(),
                findAnswerDtos(version.getId())
        );
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
