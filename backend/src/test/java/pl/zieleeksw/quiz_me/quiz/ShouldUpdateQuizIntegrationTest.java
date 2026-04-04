package pl.zieleeksw.quiz_me.quiz;

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
import pl.zieleeksw.quiz_me.question.TestQuestionAnswerRequest;
import pl.zieleeksw.quiz_me.question.TestQuestionDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnBadRequestStatus;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnOkStatus;

class ShouldUpdateQuizIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldCreateNewVersionWhenUpdatingManualQuizToCategoryQuiz() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(ownerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring Security Associate",
                "A focused course for filters, JWTs, authorization rules, and access control."
        ));
        final TestCategoryDto securityCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Security");
        final TestQuestionDto question = createQuestion(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which filter usually inspects the bearer token in a security chain?",
                List.of(
                        new TestQuestionAnswerRequest("JwtAuthenticationFilter", true),
                        new TestQuestionAnswerRequest("DispatcherServlet", false)
                ),
                List.of(securityCategory.id())
        ));
        final TestQuizDto createdQuiz = createQuiz(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "Security Foundations",
                "manual",
                null,
                "fixed",
                "fixed",
                List.of(question.id()),
                List.of()
        ));

        final ResultActions updateResult = mockMvc.perform(put("/courses/{courseId}/quizzes/{quizId}", course.id(), createdQuiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestUpdateQuizRequest(
                        "Security Drill",
                        "category",
                        null,
                        "random",
                        "random",
                        List.of(),
                        List.of(securityCategory.id())
                ))));

        final TestQuizDto updatedQuiz = readResponse(updateResult, TestQuizDto.class);
        final ResultActions versionsResult = mockMvc.perform(get("/courses/{courseId}/quizzes/{quizId}/versions", course.id(), createdQuiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuizVersionDto> versions = readVersions(versionsResult);

        itShouldReturnOkStatus(updateResult);
        itShouldReturnOkStatus(versionsResult);
        assertThat(updatedQuiz.active()).isTrue();
        assertThat(updatedQuiz.currentVersionNumber()).isEqualTo(2);
        assertThat(updatedQuiz.title()).isEqualTo("Security Drill");
        assertThat(updatedQuiz.mode()).isEqualTo("category");
        assertThat(updatedQuiz.randomCount()).isNull();
        assertThat(updatedQuiz.questionOrder()).isEqualTo("random");
        assertThat(updatedQuiz.answerOrder()).isEqualTo("random");
        assertThat(updatedQuiz.questionIds()).isEmpty();
        assertThat(updatedQuiz.categories())
                .extracting(TestQuizCategoryDto::name)
                .containsExactly("Security");

        assertThat(versions).hasSize(2);
        assertThat(versions.getFirst().versionNumber()).isEqualTo(2);
        assertThat(versions.getFirst().mode()).isEqualTo("category");
        assertThat(versions.get(1).versionNumber()).isEqualTo(1);
        assertThat(versions.get(1).mode()).isEqualTo("manual");
        assertThat(versions.get(1).questionIds()).containsExactly(question.id());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingQuizWithoutAnyChanges() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(ownerAuthentication.accessToken().value(), new TestCreateCourseRequest(
                "Spring Boot Associate",
                "A focused course for architecture, persistence, and testing drills."
        ));
        final TestCategoryDto webCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Web");
        final TestQuestionDto question = createQuestion(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuestionRequest(
                "Which bean is responsible for handling incoming REST requests?",
                List.of(
                        new TestQuestionAnswerRequest("DispatcherServlet", true),
                        new TestQuestionAnswerRequest("EntityManager", false)
                ),
                List.of(webCategory.id())
        ));
        final TestQuizDto createdQuiz = createQuiz(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "HTTP Foundations",
                "manual",
                null,
                "fixed",
                "fixed",
                List.of(question.id()),
                List.of()
        ));

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}/quizzes/{quizId}", course.id(), createdQuiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestUpdateQuizRequest(
                        "HTTP Foundations",
                        "manual",
                        null,
                        "fixed",
                        "fixed",
                        List.of(question.id()),
                        List.of()
                ))));

        final TestRuntimeExceptionDto response = readResponse(result, TestRuntimeExceptionDto.class);

        itShouldReturnBadRequestStatus(result);
        assertThat(response).isEqualTo(new TestRuntimeExceptionDto(
                "IllegalArgumentException",
                "Quiz update must change the title, mode, random settings, order settings, questions, or categories."
        ));
    }

    @Test
    void shouldArchiveQuizInsteadOfDeletingItPermanently() throws Exception {
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
        final TestQuizDto createdQuiz = createQuiz(course.id(), ownerAuthentication.accessToken().value(), new TestCreateQuizRequest(
                "Security Drill",
                "category",
                null,
                "random",
                "fixed",
                List.of(),
                List.of(securityCategory.id())
        ));

        final ResultActions archiveResult = mockMvc.perform(delete("/courses/{courseId}/quizzes/{quizId}", course.id(), createdQuiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));
        final ResultActions fetchResult = mockMvc.perform(get("/courses/{courseId}/quizzes", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));
        final ResultActions versionsResult = mockMvc.perform(get("/courses/{courseId}/quizzes/{quizId}/versions", course.id(), createdQuiz.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuizDto> quizzes = readQuizList(fetchResult);
        final List<TestQuizVersionDto> versions = readVersions(versionsResult);

        itShouldReturnOkStatus(fetchResult);
        itShouldReturnOkStatus(versionsResult);
        assertThat(archiveResult.andReturn().getResponse().getStatus()).isEqualTo(204);
        assertThat(quizzes).hasSize(1);
        assertThat(quizzes.getFirst().id()).isEqualTo(createdQuiz.id());
        assertThat(quizzes.getFirst().active()).isFalse();
        assertThat(versions).hasSize(1);
        assertThat(versions.getFirst().quizId()).isEqualTo(createdQuiz.id());
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

    private List<TestQuizVersionDto> readVersions(
            final ResultActions result
    ) throws Exception {
        return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), new TypeReference<>() {
        });
    }

    private List<TestQuizDto> readQuizList(
            final ResultActions result
    ) throws Exception {
        return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), new TypeReference<>() {
        });
    }

    private String bearerToken(final String accessToken) {
        return BEARER_PREFIX + accessToken;
    }

    private <T> T readResponse(
            final ResultActions result,
            final Class<T> responseClass
    ) throws Exception {
        return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), responseClass);
    }
}
