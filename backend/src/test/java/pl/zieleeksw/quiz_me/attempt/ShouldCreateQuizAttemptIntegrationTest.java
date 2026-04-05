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
import pl.zieleeksw.quiz_me.TestRuntimeExceptionDto;
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
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnBadRequestStatus;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnCreatedStatus;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnOkStatus;

class ShouldCreateQuizAttemptIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldCreateQuizAttemptAndPersistItForCurrentUser() throws Exception {
        final var learnerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(learnerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring HTTP Fundamentals",
                "A course focused on request handling, controllers, and response flow."
        ));
        final TestCategoryDto webCategory = createCategory(course.id(), learnerAuthentication.accessToken().value(), "Web");
        final TestQuestionDto questionOne = createQuestion(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which component dispatches incoming HTTP requests to Spring MVC?",
                List.of(
                        new TestQuestionAnswerRequest("DispatcherServlet", true),
                        new TestQuestionAnswerRequest("EntityManager", false)
                ),
                List.of(webCategory.id())
        ));
        final TestQuestionDto questionTwo = createQuestion(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which annotation marks a controller method for GET requests?",
                List.of(
                        new TestQuestionAnswerRequest("@GetMapping", true),
                        new TestQuestionAnswerRequest("@Bean", false)
                ),
                List.of(webCategory.id())
        ));
        final TestQuizDto quiz = createQuiz(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "HTTP Drill",
                "manual",
                null,
                "fixed",
                "fixed",
                List.of(questionOne.id(), questionTwo.id()),
                List.of()
        ));

        final ResultActions createAttemptResult = mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/attempts", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestSubmitQuizAttemptRequest(List.of(
                        answer(questionOne, "DispatcherServlet"),
                        answer(questionTwo, "@Bean")
                )))));
        final ResultActions fetchAttemptsResult = mockMvc.perform(get("/courses/{courseId}/attempts", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final TestQuizAttemptDto createdAttempt = readResponse(createAttemptResult, TestQuizAttemptDto.class);
        final List<TestQuizAttemptDto> attempts = readAttempts(fetchAttemptsResult);

        itShouldReturnCreatedStatus(createAttemptResult);
        itShouldReturnOkStatus(fetchAttemptsResult);
        assertThat(createdAttempt.id()).isNotNull();
        assertThat(createdAttempt.courseId()).isEqualTo(course.id());
        assertThat(createdAttempt.quizId()).isEqualTo(quiz.id());
        assertThat(createdAttempt.userId()).isNotNull();
        assertThat(createdAttempt.quizTitle()).isEqualTo("HTTP Drill");
        assertThat(createdAttempt.correctAnswers()).isEqualTo(1);
        assertThat(createdAttempt.totalQuestions()).isEqualTo(2);
        assertThat(createdAttempt.finishedAt()).isNotNull();
        assertThat(attempts).hasSize(1);
        assertThat(attempts.getFirst()).isEqualTo(createdAttempt);
    }

    @Test
    void shouldReturnOnlyAttemptsOfCurrentUserForTheCourse() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final var secondLearnerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(ownerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring Security Core",
                "A course focused on filters, JWTs, and access rules."
        ));
        final TestCategoryDto securityCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Security");
        final TestQuestionDto question = createQuestion(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which filter usually inspects the bearer token?",
                List.of(
                        new TestQuestionAnswerRequest("JwtAuthenticationFilter", true),
                        new TestQuestionAnswerRequest("DispatcherServlet", false)
                ),
                List.of(securityCategory.id())
        ));
        final TestQuizDto quiz = createQuiz(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "Security Basics",
                "manual",
                null,
                "fixed",
                "fixed",
                List.of(question.id()),
                List.of()
        ));

        mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/attempts", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestSubmitQuizAttemptRequest(List.of(
                        answer(question, "JwtAuthenticationFilter")
                )))));
        mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/attempts", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(secondLearnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestSubmitQuizAttemptRequest(List.of(
                        answer(question, "DispatcherServlet")
                )))));

        final ResultActions fetchOwnAttemptsResult = mockMvc.perform(get("/courses/{courseId}/attempts", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuizAttemptDto> attempts = readAttempts(fetchOwnAttemptsResult);

        itShouldReturnOkStatus(fetchOwnAttemptsResult);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.getFirst().correctAnswers()).isEqualTo(1);
        assertThat(attempts.getFirst().totalQuestions()).isEqualTo(1);
    }

    @Test
    void shouldReturnBadRequestWhenAttemptDoesNotCoverWholeQuiz() throws Exception {
        final var learnerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(learnerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring Persistence Foundations",
                "A course focused on entities, repositories, and transactions."
        ));
        final TestCategoryDto persistenceCategory = createCategory(course.id(), learnerAuthentication.accessToken().value(), "Persistence");
        final TestQuestionDto questionOne = createQuestion(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which abstraction usually wraps database transactions?",
                List.of(
                        new TestQuestionAnswerRequest("PlatformTransactionManager", true),
                        new TestQuestionAnswerRequest("DispatcherServlet", false)
                ),
                List.of(persistenceCategory.id())
        ));
        final TestQuestionDto questionTwo = createQuestion(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which annotation marks a JPA entity?",
                List.of(
                        new TestQuestionAnswerRequest("@Entity", true),
                        new TestQuestionAnswerRequest("@Repository", false)
                ),
                List.of(persistenceCategory.id())
        ));
        final TestQuizDto quiz = createQuiz(course.id(), learnerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "Persistence Drill",
                "manual",
                null,
                "fixed",
                "fixed",
                List.of(questionOne.id(), questionTwo.id()),
                List.of()
        ));

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/quizzes/{quizId}/attempts", course.id(), quiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestSubmitQuizAttemptRequest(List.of(
                        answer(questionOne, "PlatformTransactionManager")
                )))));

        final TestRuntimeExceptionDto response = readResponse(result, TestRuntimeExceptionDto.class);

        itShouldReturnBadRequestStatus(result);
        assertThat(response).isEqualTo(new TestRuntimeExceptionDto(
                "IllegalArgumentException",
                "Quiz attempt must answer every quiz question exactly once."
        ));
    }

    private TestQuizAttemptAnswerRequest answer(
            final TestQuestionDto question,
            final String answerContent
    ) {
        final TestQuestionAnswerDto selectedAnswer = question.answers()
                .stream()
                .filter(answer -> answer.content().equals(answerContent))
                .findFirst()
                .orElseThrow();

        return new TestQuizAttemptAnswerRequest(question.id(), selectedAnswer.id());
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

    private List<TestQuizAttemptDto> readAttempts(
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
