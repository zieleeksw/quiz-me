package pl.zieleeksw.quiz_me.attempt.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import pl.zieleeksw.quiz_me.attempt.QuizAttemptAnswerRequest;
import pl.zieleeksw.quiz_me.attempt.QuizSessionDto;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.QuestionDto;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;
import pl.zieleeksw.quiz_me.quiz.QuizDto;
import pl.zieleeksw.quiz_me.quiz.QuizMode;
import pl.zieleeksw.quiz_me.quiz.domain.QuizFacade;
import pl.zieleeksw.quiz_me.quiz.domain.QuizNotFoundException;

import java.time.Instant;
import java.util.*;

public class QuizSessionFacade {

    private final QuizSessionRepository quizSessionRepository;
    private final CourseFacade courseFacade;
    private final QuizFacade quizFacade;
    private final QuestionFacade questionFacade;
    private final ObjectMapper objectMapper;

    QuizSessionFacade(
            final QuizSessionRepository quizSessionRepository,
            final CourseFacade courseFacade,
            final QuizFacade quizFacade,
            final QuestionFacade questionFacade,
            final ObjectMapper objectMapper
    ) {
        this.quizSessionRepository = quizSessionRepository;
        this.courseFacade = courseFacade;
        this.quizFacade = quizFacade;
        this.questionFacade = questionFacade;
        this.objectMapper = objectMapper;
    }

    public List<QuizSessionDto> fetchCourseSessions(
            final Long courseId,
            final Long userId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);

        return quizSessionRepository.findAllByCourseIdAndUserIdOrderByUpdatedAtDesc(courseId, userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public QuizSessionDto createOrResumeSession(
            final Long courseId,
            final Long quizId,
            final Long userId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);
        final QuizDto quiz = findActiveQuiz(courseId, quizId);

        return quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
                .map(this::toDto)
                .orElseGet(() -> createSession(courseId, quiz, userId));
    }

    @Transactional
    public QuizSessionDto updateSession(
            final Long courseId,
            final Long quizId,
            final Long userId,
            final int currentIndex,
            final List<QuizAttemptAnswerRequest> answers
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);
        findActiveQuiz(courseId, quizId);

        final QuizSessionEntity entity = quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
                .orElseThrow(() -> QuizSessionNotFoundException.forQuizId(quizId));
        final List<Long> questionIds = deserializeQuestionIds(entity.getQuestionIdsJson());
        final Map<Long, Long> normalizedAnswers = normalizeSessionAnswers(questionIds, answers);
        assertAnswersBelongToQuestions(courseId, normalizedAnswers);

        if (questionIds.isEmpty()) {
            throw new IllegalArgumentException("Quiz session does not contain any playable questions.");
        }

        final int boundedIndex = Math.max(0, Math.min(currentIndex, questionIds.size() - 1));
        final Instant now = roundToDatabasePrecision(Instant.now());
        final QuizSession session = QuizSession.restore(
                entity.getId(),
                entity.getCourseId(),
                entity.getQuizId(),
                entity.getUserId(),
                entity.getQuizTitle(),
                entity.getQuestionIdsJson(),
                entity.getAnswersJson(),
                entity.getCurrentIndex(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        session.updateProgress(serializeAnswers(normalizedAnswers), boundedIndex, now);

        final QuizSessionEntity saved = quizSessionRepository.save(QuizSessionEntity.from(session));
        return toDto(saved);
    }

    @Transactional
    public void deleteSession(
            final Long courseId,
            final Long quizId,
            final Long userId
    ) {
        quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
                .ifPresent(quizSessionRepository::delete);
    }

    private QuizSessionDto createSession(
            final Long courseId,
            final QuizDto quiz,
            final Long userId
    ) {
        final List<QuestionDto> currentQuestions = questionFacade.fetchQuestions(courseId);
        final List<Long> resolvedQuestionIds = resolveQuestionIdsForQuiz(quiz, currentQuestions);

        if (resolvedQuestionIds.isEmpty()) {
            throw new IllegalArgumentException("Quiz does not contain any playable questions.");
        }

        final Instant now = roundToDatabasePrecision(Instant.now());
        final QuizSession session = QuizSession.create(
                courseId,
                quiz.id(),
                userId,
                quiz.title(),
                serializeQuestionIds(resolvedQuestionIds),
                serializeAnswers(Map.of()),
                0,
                now
        );
        final QuizSessionEntity saved = quizSessionRepository.save(QuizSessionEntity.from(session));

        return toDto(saved);
    }

    private QuizDto findActiveQuiz(
            final Long courseId,
            final Long quizId
    ) {
        final QuizDto quiz = quizFacade.fetchQuizzes(courseId)
                .stream()
                .filter(candidate -> candidate.id().equals(quizId))
                .findFirst()
                .orElseThrow(() -> QuizNotFoundException.forId(quizId));

        if (!quiz.active()) {
            throw QuizNotFoundException.forId(quizId);
        }

        return quiz;
    }

    private Map<Long, Long> normalizeSessionAnswers(
            final List<Long> questionIds,
            final List<QuizAttemptAnswerRequest> answers
    ) {
        if (answers == null || answers.isEmpty()) {
            return Map.of();
        }

        final Set<Long> allowedQuestionIds = new LinkedHashSet<>(questionIds);
        final Map<Long, Long> normalizedAnswers = new LinkedHashMap<>();

        for (final QuizAttemptAnswerRequest answer : answers) {
            if (answer == null) {
                throw new IllegalArgumentException("Quiz session answers cannot contain empty values.");
            }

            if (!allowedQuestionIds.contains(answer.questionId())) {
                throw new IllegalArgumentException("Quiz session contains questions outside the selected quiz.");
            }

            if (normalizedAnswers.putIfAbsent(answer.questionId(), answer.answerId()) != null) {
                throw new IllegalArgumentException("Quiz session cannot contain duplicate question answers.");
            }
        }

        return normalizedAnswers;
    }

    private List<Long> resolveQuestionIdsForQuiz(
            final QuizDto quiz,
            final List<QuestionDto> currentQuestions
    ) {
        if (quiz.mode() == QuizMode.MANUAL) {
            final Set<Long> availableQuestionIds = currentQuestions.stream()
                    .map(QuestionDto::id)
                    .collect(LinkedHashSet::new, Set::add, Set::addAll);
            final List<Long> manualQuestionIds = quiz.questionIds().stream()
                    .filter(availableQuestionIds::contains)
                    .toList();

            return quiz.questionOrder().name().equals("RANDOM") ? shuffleIds(manualQuestionIds) : manualQuestionIds;
        }

        if (quiz.mode() == QuizMode.RANDOM) {
            final List<Long> courseQuestionIds = currentQuestions.stream()
                    .map(QuestionDto::id)
                    .toList();
            final List<Long> shuffledQuestionIds = shuffleIds(courseQuestionIds);
            final List<Long> selectedQuestionIds = shuffledQuestionIds.subList(0, Math.min(quiz.randomCount() == null ? shuffledQuestionIds.size() : quiz.randomCount(), shuffledQuestionIds.size()));

            if (quiz.questionOrder().name().equals("RANDOM")) {
                return List.copyOf(selectedQuestionIds);
            }

            final Set<Long> selectedQuestionIdSet = new LinkedHashSet<>(selectedQuestionIds);
            return courseQuestionIds.stream()
                    .filter(selectedQuestionIdSet::contains)
                    .toList();
        }

        final Set<Long> categoryIds = quiz.categories().stream()
                .map(category -> category.id())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        final List<Long> categoryQuestionIds = currentQuestions.stream()
                .filter(question -> question.categories().stream().anyMatch(category -> categoryIds.contains(category.id())))
                .map(QuestionDto::id)
                .toList();

        return quiz.questionOrder().name().equals("RANDOM") ? shuffleIds(categoryQuestionIds) : categoryQuestionIds;
    }

    private List<Long> shuffleIds(
            final List<Long> questionIds
    ) {
        final List<Long> shuffledQuestionIds = new ArrayList<>(questionIds);
        Collections.shuffle(shuffledQuestionIds);
        return List.copyOf(shuffledQuestionIds);
    }

    private void assertAnswersBelongToQuestions(
            final Long courseId,
            final Map<Long, Long> answers
    ) {
        if (answers.isEmpty()) {
            return;
        }

        final Map<Long, QuestionDto> questionsById = questionFacade.fetchQuestions(courseId).stream()
                .collect(LinkedHashMap::new, (map, question) -> map.put(question.id(), question), Map::putAll);

        for (final Map.Entry<Long, Long> answerEntry : answers.entrySet()) {
            final QuestionDto question = questionsById.get(answerEntry.getKey());

            if (question == null || question.answers().stream().noneMatch(answer -> answer.id().equals(answerEntry.getValue()))) {
                throw new IllegalArgumentException("Quiz session contains answers outside the selected quiz.");
            }
        }
    }

    private QuizSessionDto toDto(
            final QuizSessionEntity entity
    ) {
        return new QuizSessionDto(
                entity.getId(),
                entity.getCourseId(),
                entity.getQuizId(),
                entity.getUserId(),
                entity.getQuizTitle(),
                deserializeQuestionIds(entity.getQuestionIdsJson()),
                entity.getCurrentIndex(),
                deserializeAnswers(entity.getAnswersJson()),
                entity.getUpdatedAt()
        );
    }

    private String serializeQuestionIds(
            final List<Long> questionIds
    ) {
        return writeJson(questionIds);
    }

    private List<Long> deserializeQuestionIds(
            final String questionIdsJson
    ) {
        return readJson(questionIdsJson, new TypeReference<>() {
        });
    }

    private String serializeAnswers(
            final Map<Long, Long> answers
    ) {
        return writeJson(answers);
    }

    private Map<Long, Long> deserializeAnswers(
            final String answersJson
    ) {
        final Map<Long, Long> answers = readJson(answersJson, new TypeReference<>() {
        });
        return answers == null ? Map.of() : answers;
    }

    private String writeJson(
            final Object value
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize quiz session state.", ex);
        }
    }

    private <T> T readJson(
            final String value,
            final TypeReference<T> typeReference
    ) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize quiz session state.", ex);
        }
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
}
