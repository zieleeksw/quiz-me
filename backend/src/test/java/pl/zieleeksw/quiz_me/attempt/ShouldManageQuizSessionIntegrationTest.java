package pl.zieleeksw.quiz_me.attempt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;
import pl.zieleeksw.quiz_me.category.TestCategoryDto;
import pl.zieleeksw.quiz_me.category.TestCreateCategoryRequest;
import pl.zieleeksw.quiz_me.course.TestCourseDto;
import pl.zieleeksw.quiz_me.course.TestCreateCourseRequest;
import pl.zieleeksw.quiz_me.question.TestCreateQuestionRequest;
import pl.zieleeksw.quiz_me.question.TestQuestionAnswerDto;
import pl.zieleeksw.quiz_me.question.TestQuestionAnswerRequest;
import pl.zieleeksw.quiz_me.question.TestQuestionDto;
import pl.zieleeksw.quiz_me.quiz.TestCreateQuizRequest;
import pl.zieleeksw.quiz_me.quiz.TestQuizDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnOkStatus;

class ShouldManageQuizSessionIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldCreateAndResumeExistingQuizSession() throws Exception {
        final var learnerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(learnerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring MVC Flow",
                "A course focused on controllers, mappings, and request handling."
        ));
        final TestCategoryDto webCategory = createCategory(course.id(), learnerAuthentication.accessToken().value(), "Web");
        final TestQuestionDto questionOne = createQuestion(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which servlet dispatches Spring MVC requests?",
                List.of(
                        new TestQuestionAnswerRequest("DispatcherServlet", true),
                        new TestQuestionAnswerRequest("EntityManager", false)
                ),
                List.of(webCategory.id())
        ));
        final TestQuestionDto questionTwo = createQuestion(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which annotation maps GET requests?",
                List.of(
                        new TestQuestionAnswerRequest("@GetMapping", true),
                        new TestQuestionAnswerRequest("@Bean", false)
                ),
                List.of(webCategory.id())
        ));
        final TestQuizDto quiz = createQuiz(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "MVC Drill",
                "manual",
                null,
                "fixed",
                "fixed",
                List.of(questionOne.id(), questionTwo.id()),
                List.of()
        ));

        final ResultActions createSessionResult = mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/session", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));
        final TestQuizSessionDto createdSession = readResponse(createSessionResult, TestQuizSessionDto.class);

        final ResultActions updateSessionResult = mockMvc.perform(put("/courses/{courseId}/quizzes/{quizId}/session", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestUpdateQuizSessionRequest(
                        1,
                        List.of(answer(questionOne, "DispatcherServlet"))
                ))));
        final TestQuizSessionDto updatedSession = readResponse(updateSessionResult, TestQuizSessionDto.class);

        final ResultActions resumeSessionResult = mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/session", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));
        final ResultActions fetchSessionsResult = mockMvc.perform(get("/courses/{courseId}/sessions", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final TestQuizSessionDto resumedSession = readResponse(resumeSessionResult, TestQuizSessionDto.class);
        final List<TestQuizSessionDto> sessions = readSessions(fetchSessionsResult);

        itShouldReturnOkStatus(createSessionResult);
        itShouldReturnOkStatus(updateSessionResult);
        itShouldReturnOkStatus(resumeSessionResult);
        itShouldReturnOkStatus(fetchSessionsResult);
        assertThat(createdSession.questionIds()).containsExactly(questionOne.id(), questionTwo.id());
        assertThat(createdSession.currentIndex()).isZero();
        assertThat(createdSession.answers()).isEmpty();
        assertThat(updatedSession.id()).isEqualTo(createdSession.id());
        assertThat(updatedSession.currentIndex()).isEqualTo(1);
        assertThat(updatedSession.answers()).containsEntry(questionOne.id(), answerId(questionOne, "DispatcherServlet"));
        assertThat(resumedSession).isEqualTo(updatedSession);
        assertThat(sessions).containsExactly(updatedSession);
    }

    @Test
    void shouldRemoveSessionAfterCompletingQuizAttempt() throws Exception {
        final var learnerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(learnerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring Data Flow",
                "A course focused on repositories and persistence."
        ));
        final TestCategoryDto persistenceCategory = createCategory(course.id(), learnerAuthentication.accessToken().value(), "Persistence");
        final TestQuestionDto question = createQuestion(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which interface is commonly extended for CRUD access?",
                List.of(
                        new TestQuestionAnswerRequest("JpaRepository", true),
                        new TestQuestionAnswerRequest("Filter", false)
                ),
                List.of(persistenceCategory.id())
        ));
        final TestQuizDto quiz = createQuiz(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "Persistence Basics",
                "manual",
                null,
                "fixed",
                "fixed",
                List.of(question.id()),
                List.of()
        ));

        mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/session", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));
        mockMvc.perform(put("/courses/{courseId}/quizzes/{quizId}/session", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestUpdateQuizSessionRequest(
                        0,
                        List.of(answer(question, "JpaRepository"))
                ))));

        final ResultActions completeAttemptResult = mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/attempts", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestSubmitQuizAttemptRequest(List.of(
                        answer(question, "JpaRepository")
                )))));
        final ResultActions fetchSessionsResult = mockMvc.perform(get("/courses/{courseId}/sessions", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final TestQuizAttemptDto attempt = readResponse(completeAttemptResult, TestQuizAttemptDto.class);
        final List<TestQuizSessionDto> sessions = readSessions(fetchSessionsResult);

        itShouldReturnOkStatus(fetchSessionsResult);
        assertThat(attempt.correctAnswers()).isEqualTo(1);
        assertThat(sessions).isEmpty();
    }

    private TestQuizAttemptAnswerRequest answer(
            final TestQuestionDto question,
            final String answerContent
    ) {
        return new TestQuizAttemptAnswerRequest(question.id(), answerId(question, answerContent));
    }

    private Long answerId(
            final TestQuestionDto question,
            final String answerContent
    ) {
        final TestQuestionAnswerDto selectedAnswer = question.answers()
                .stream()
                .filter(answer -> answer.content().equals(answerContent))
                .findFirst()
                .orElseThrow();

        return selectedAnswer.id();
    }

    private TestCategoryDto createCategory(
            final Long courseId,
            final String accessToken,
            final String name
    ) throws Exception {
        final MvcResult result = mockMvc.perform(post("/courses/{courseId}/categories", courseId)
                        .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestCreateCategoryRequest(name))))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TestCategoryDto.class);
    }

    private TestCourseDto createCourse(
            final String accessToken,
            final TestCreateCourseRequest request
    ) throws Exception {
        final MvcResult result = mockMvc.perform(post("/courses")
                        .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TestCourseDto.class);
    }

    private TestQuestionDto createQuestion(
            final Long courseId,
            final String accessToken,
            final TestCreateQuestionRequest request
    ) throws Exception {
        final MvcResult result = mockMvc.perform(post("/courses/{courseId}/questions", courseId)
                        .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TestQuestionDto.class);
    }

    private TestQuizDto createQuiz(
            final Long courseId,
            final String accessToken,
            final TestCreateQuizRequest request
    ) throws Exception {
        final MvcResult result = mockMvc.perform(post("/courses/{courseId}/quizzes", courseId)
                        .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TestQuizDto.class);
    }

    private List<TestQuizSessionDto> readSessions(
            final ResultActions result
    ) throws Exception {
        return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), new TypeReference<>() {
        });
    }

    private String bearerToken(
            final String accessToken
    ) {
        return BEARER_PREFIX + accessToken;
    }

    private <T> T readResponse(
            final ResultActions result,
            final Class<T> responseClass
    ) throws Exception {
        return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), responseClass);
    }
}
