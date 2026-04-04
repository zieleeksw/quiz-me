package pl.zieleeksw.quiz_me.quiz;

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
import pl.zieleeksw.quiz_me.question.TestQuestionAnswerRequest;
import pl.zieleeksw.quiz_me.question.TestQuestionDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldCreateQuizIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldReturnUnauthorizedWhenCreatingQuizWithoutToken() throws Exception {
        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/quizzes", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validManualQuizRequest(List.of(1L)))));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldCreateManualQuizAsCourseOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(ownerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring Boot Associate",
                "A focused course for architecture, persistence, and testing drills."
        ));
        final TestCategoryDto webCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Web");
        final TestQuestionDto questionOne = createQuestion(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which bean is responsible for handling incoming REST requests?",
                List.of(
                        new TestQuestionAnswerRequest("DispatcherServlet", true),
                        new TestQuestionAnswerRequest("EntityManager", false)
                ),
                List.of(webCategory.id())
        ));
        final TestQuestionDto questionTwo = createQuestion(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which component maps HTTP requests onto controller methods?",
                List.of(
                        new TestQuestionAnswerRequest("RequestMappingHandlerMapping", true),
                        new TestQuestionAnswerRequest("TaskScheduler", false)
                ),
                List.of(webCategory.id())
        ));

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/quizzes", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validManualQuizRequest(List.of(questionOne.id(), questionTwo.id())))));

        final TestQuizDto response = readResponse(result, TestQuizDto.class);

        itShouldReturnCreatedStatus(result);
        assertThat(response.id()).isNotNull();
        assertThat(response.courseId()).isEqualTo(course.id());
        assertThat(response.currentVersionNumber()).isEqualTo(1);
        assertThat(response.title()).isEqualTo("HTTP Foundations");
        assertThat(response.mode()).isEqualTo("manual");
        assertThat(response.randomCount()).isNull();
        assertThat(response.questionOrder()).isEqualTo("fixed");
        assertThat(response.answerOrder()).isEqualTo("random");
        assertThat(response.questionIds()).containsExactly(questionOne.id(), questionTwo.id());
        assertThat(response.categories()).isEmpty();
    }

    @Test
    void shouldCreateRandomQuizWithCategoryFiltersAsCourseOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(ownerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring Security Associate",
                "A focused course for filters, JWTs, authorization rules, and access control."
        ));
        final TestCategoryDto securityCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Security");
        createQuestion(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which filter usually inspects the bearer token in a security chain?",
                List.of(
                        new TestQuestionAnswerRequest("JwtAuthenticationFilter", true),
                        new TestQuestionAnswerRequest("DispatcherServlet", false)
                ),
                List.of(securityCategory.id())
        ));

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/quizzes", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestCreateQuizRequest(
                        "Security Drill",
                        "random",
                        5,
                        "random",
                        "fixed",
                        List.of(),
                        List.of(securityCategory.id())
                ))));

        final TestQuizDto response = readResponse(result, TestQuizDto.class);

        itShouldReturnCreatedStatus(result);
        assertThat(response.mode()).isEqualTo("random");
        assertThat(response.randomCount()).isEqualTo(5);
        assertThat(response.questionIds()).isEmpty();
        assertThat(response.categories())
                .extracting(TestQuizCategoryDto::name)
                .containsExactly("Security");
    }

    private TestCreateQuizRequest validManualQuizRequest(final List<Long> questionIds) {
        return new TestCreateQuizRequest(
                "HTTP Foundations",
                "manual",
                null,
                "fixed",
                "random",
                questionIds,
                List.of()
        );
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

    private String bearerToken(final String accessToken) {
        return BEARER_PREFIX + accessToken;
    }

    private <T> T readResponse(
            final ResultActions result,
            final Class<T> responseClass
    ) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), responseClass);
    }
}
