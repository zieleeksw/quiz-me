package pl.zieleeksw.quiz_me.attempt.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import pl.zieleeksw.quiz_me.attempt.QuizAttemptAnswerRequest;
import pl.zieleeksw.quiz_me.attempt.QuizAttemptAnswerReviewDto;
import pl.zieleeksw.quiz_me.attempt.QuizAttemptDetailDto;
import pl.zieleeksw.quiz_me.attempt.QuizAttemptDto;
import pl.zieleeksw.quiz_me.attempt.QuizAttemptQuestionReviewDto;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.QuestionAnswerDto;
import pl.zieleeksw.quiz_me.question.QuestionDto;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;
import pl.zieleeksw.quiz_me.quiz.QuizDto;
import pl.zieleeksw.quiz_me.quiz.QuizMode;
import pl.zieleeksw.quiz_me.quiz.domain.QuizFacade;
import pl.zieleeksw.quiz_me.quiz.domain.QuizNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class QuizAttemptFacade {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final CourseFacade courseFacade;
    private final QuizFacade quizFacade;
    private final QuestionFacade questionFacade;
    private final ObjectMapper objectMapper;

    QuizAttemptFacade(
            final QuizAttemptRepository quizAttemptRepository,
            final QuizSessionRepository quizSessionRepository,
            final CourseFacade courseFacade,
            final QuizFacade quizFacade,
            final QuestionFacade questionFacade,
            final ObjectMapper objectMapper
    ) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.courseFacade = courseFacade;
        this.quizFacade = quizFacade;
        this.questionFacade = questionFacade;
        this.objectMapper = objectMapper;
    }

    public List<QuizAttemptDto> fetchCourseAttempts(
            final Long courseId,
            final Long userId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);

        return quizAttemptRepository.findAllByCourseIdAndUserIdOrderByFinishedAtDesc(courseId, userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public QuizAttemptDetailDto fetchAttemptDetail(
            final Long courseId,
            final Long attemptId,
            final Long userId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);

        final QuizAttemptEntity entity = quizAttemptRepository.findByIdAndCourseIdAndUserId(attemptId, courseId, userId)
                .orElseThrow(() -> QuizAttemptNotFoundException.forId(attemptId));

        return toDetailDto(entity);
    }

    @Transactional
    public QuizAttemptDto createAttempt(
            final Long courseId,
            final Long quizId,
            final Long userId,
            final List<QuizAttemptAnswerRequest> answers
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);

        final QuizDto quiz = quizFacade.fetchQuizzes(courseId)
                .stream()
                .filter(candidate -> candidate.id().equals(quizId))
                .findFirst()
                .orElseThrow(() -> QuizNotFoundException.forId(quizId));

        if (!quiz.active()) {
            throw QuizNotFoundException.forId(quizId);
        }

        final Map<Long, Long> submittedAnswers = normalizeSubmittedAnswers(answers);
        final List<QuestionDto> currentQuestions = questionFacade.fetchQuestions(courseId);
        final Map<Long, QuestionDto> questionsById = currentQuestions.stream()
                .collect(LinkedHashMap::new, (map, question) -> map.put(question.id(), question), Map::putAll);
        final Optional<QuizSessionEntity> activeSession = quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId);
        final AttemptQuestionSpec questionSpec = activeSession
                .map(session -> {
                    final List<Long> sessionQuestionIds = deserializeQuestionIds(session.getQuestionIdsJson());
                    return new AttemptQuestionSpec(new LinkedHashSet<>(sessionQuestionIds), sessionQuestionIds.size());
                })
                .orElseGet(() -> resolveQuestionSpec(quiz, currentQuestions));

        assertSubmittedQuestionsMatchQuiz(quiz, submittedAnswers.keySet(), questionSpec);

        final int correctAnswers = submittedAnswers.entrySet()
                .stream()
                .mapToInt(entry -> isCorrectAnswer(questionsById, entry.getKey(), entry.getValue()) ? 1 : 0)
                .sum();
        final List<Long> orderedQuestionIds = activeSession
                .map(session -> deserializeQuestionIds(session.getQuestionIdsJson()))
                .orElseGet(() -> new ArrayList<>(submittedAnswers.keySet()));
        final String reviewSnapshotJson = serializeReviewSnapshot(buildReviewSnapshot(orderedQuestionIds, submittedAnswers, questionsById));

        final Instant finishedAt = roundToDatabasePrecision(Instant.now());
        final QuizAttempt attempt = QuizAttempt.create(
                courseId,
                quiz.id(),
                userId,
                activeSession.map(QuizSessionEntity::getQuizTitle).orElse(quiz.title()),
                correctAnswers,
                questionSpec.expectedCount(),
                reviewSnapshotJson,
                finishedAt
        );
        final QuizAttemptEntity saved = quizAttemptRepository.save(QuizAttemptEntity.from(attempt));
        activeSession.ifPresent(quizSessionRepository::delete);

        return toDto(saved);
    }

    private Map<Long, Long> normalizeSubmittedAnswers(
            final List<QuizAttemptAnswerRequest> answers
    ) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Quiz attempt must contain at least 1 answer.");
        }

        final Map<Long, Long> submittedAnswers = new LinkedHashMap<>();

        for (final QuizAttemptAnswerRequest answer : answers) {
            if (answer == null) {
                throw new IllegalArgumentException("Quiz attempt answers cannot contain empty values.");
            }

            if (submittedAnswers.putIfAbsent(answer.questionId(), answer.answerId()) != null) {
                throw new IllegalArgumentException("Quiz attempt cannot contain duplicate question answers.");
            }
        }

        return submittedAnswers;
    }

    private AttemptQuestionSpec resolveQuestionSpec(
            final QuizDto quiz,
            final List<QuestionDto> currentQuestions
    ) {
        if (quiz.mode() == QuizMode.MANUAL) {
            final Set<Long> questionIds = new LinkedHashSet<>(quiz.questionIds());
            return new AttemptQuestionSpec(questionIds, questionIds.size());
        }

        if (quiz.mode() == QuizMode.RANDOM) {
            final int randomCount = Math.min(quiz.randomCount() == null ? currentQuestions.size() : quiz.randomCount(), currentQuestions.size());
            final Set<Long> allowedQuestionIds = currentQuestions.stream()
                    .map(QuestionDto::id)
                    .collect(LinkedHashSet::new, Set::add, Set::addAll);

            return new AttemptQuestionSpec(allowedQuestionIds, randomCount);
        }

        final Set<Long> categoryIds = quiz.categories()
                .stream()
                .map(category -> category.id())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        final Set<Long> questionIds = currentQuestions.stream()
                .filter(question -> question.categories()
                        .stream()
                        .anyMatch(category -> categoryIds.contains(category.id())))
                .map(QuestionDto::id)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        return new AttemptQuestionSpec(questionIds, questionIds.size());
    }

    private void assertSubmittedQuestionsMatchQuiz(
            final QuizDto quiz,
            final Set<Long> submittedQuestionIds,
            final AttemptQuestionSpec questionSpec
    ) {
        if (submittedQuestionIds.size() != questionSpec.expectedCount()) {
            throw new IllegalArgumentException("Quiz attempt must answer every quiz question exactly once.");
        }

        if (quiz.mode() == QuizMode.RANDOM) {
            if (!questionSpec.allowedQuestionIds().containsAll(submittedQuestionIds)) {
                throw new IllegalArgumentException("Quiz attempt contains questions outside the selected quiz.");
            }

            return;
        }

        if (!submittedQuestionIds.equals(questionSpec.allowedQuestionIds())) {
            throw new IllegalArgumentException("Quiz attempt contains questions outside the selected quiz.");
        }
    }

    private boolean isCorrectAnswer(
            final Map<Long, QuestionDto> questionsById,
            final Long questionId,
            final Long answerId
    ) {
        final QuestionDto question = questionsById.get(questionId);

        if (question == null) {
            throw new IllegalArgumentException("Quiz attempt contains questions outside the selected quiz.");
        }

        final QuestionAnswerDto selectedAnswer = question.answers()
                .stream()
                .filter(answer -> answer.id().equals(answerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Submitted answer does not belong to the selected question."));

        return selectedAnswer.correct();
    }

    private QuizAttemptDto toDto(
            final QuizAttemptEntity entity
    ) {
        return new QuizAttemptDto(
                entity.getId(),
                entity.getCourseId(),
                entity.getQuizId(),
                entity.getUserId(),
                entity.getQuizTitle(),
                entity.getCorrectAnswers(),
                entity.getTotalQuestions(),
                entity.getFinishedAt()
        );
    }

    private QuizAttemptDetailDto toDetailDto(
            final QuizAttemptEntity entity
    ) {
        return new QuizAttemptDetailDto(
                entity.getId(),
                entity.getCourseId(),
                entity.getQuizId(),
                entity.getUserId(),
                entity.getQuizTitle(),
                entity.getCorrectAnswers(),
                entity.getTotalQuestions(),
                entity.getFinishedAt(),
                deserializeReviewSnapshot(entity.getReviewSnapshotJson()).stream()
                        .map(this::toQuestionReviewDto)
                        .toList()
        );
    }

    private List<ReviewQuestionSnapshot> buildReviewSnapshot(
            final List<Long> orderedQuestionIds,
            final Map<Long, Long> submittedAnswers,
            final Map<Long, QuestionDto> questionsById
    ) {
        return orderedQuestionIds.stream()
                .filter(submittedAnswers::containsKey)
                .map(questionId -> {
                    final QuestionDto question = questionsById.get(questionId);

                    if (question == null) {
                        throw new IllegalArgumentException("Quiz attempt contains questions outside the selected quiz.");
                    }

                    final Long selectedAnswerId = submittedAnswers.get(questionId);
                    final Long correctAnswerId = question.answers().stream()
                            .filter(QuestionAnswerDto::correct)
                            .map(QuestionAnswerDto::id)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Question must have a correct answer."));

                    return new ReviewQuestionSnapshot(
                            question.id(),
                            question.prompt(),
                            selectedAnswerId,
                            correctAnswerId,
                            selectedAnswerId.equals(correctAnswerId),
                            question.answers().stream()
                                    .map(answer -> new ReviewAnswerSnapshot(
                                            answer.id(),
                                            answer.displayOrder(),
                                            answer.content(),
                                            answer.correct()
                                    ))
                                    .toList()
                    );
                })
                .toList();
    }

    private String serializeReviewSnapshot(
            final List<ReviewQuestionSnapshot> snapshot
    ) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize quiz attempt review snapshot.", ex);
        }
    }

    private List<ReviewQuestionSnapshot> deserializeReviewSnapshot(
            final String reviewSnapshotJson
    ) {
        try {
            return objectMapper.readValue(reviewSnapshotJson, new TypeReference<>() {
            });
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize quiz attempt review snapshot.", ex);
        }
    }

    private QuizAttemptQuestionReviewDto toQuestionReviewDto(
            final ReviewQuestionSnapshot snapshot
    ) {
        return new QuizAttemptQuestionReviewDto(
                snapshot.questionId(),
                snapshot.prompt(),
                snapshot.selectedAnswerId(),
                snapshot.correctAnswerId(),
                snapshot.answeredCorrectly(),
                snapshot.answers().stream()
                        .map(answer -> new QuizAttemptAnswerReviewDto(
                                answer.id(),
                                answer.displayOrder(),
                                answer.content(),
                                answer.correct()
                        ))
                        .toList()
        );
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

    private List<Long> deserializeQuestionIds(
            final String questionIdsJson
    ) {
        try {
            return objectMapper.readValue(questionIdsJson, new TypeReference<>() {
            });
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize quiz session questions.", ex);
        }
    }

    private record AttemptQuestionSpec(
            Set<Long> allowedQuestionIds,
            int expectedCount
    ) {
    }

    private record ReviewQuestionSnapshot(
            Long questionId,
            String prompt,
            Long selectedAnswerId,
            Long correctAnswerId,
            boolean answeredCorrectly,
            List<ReviewAnswerSnapshot> answers
    ) {
    }

    private record ReviewAnswerSnapshot(
            Long id,
            int displayOrder,
            String content,
            boolean correct
    ) {
    }
}
