package pl.zieleeksw.quiz_me.question;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldUpdateQuestionIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldCreateNewVersionWhenUpdatingQuestionAsOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Security Associate",
                        "A focused course for filters, JWTs, authorization rules, and access control."
                )
        );
        final TestCategoryDto httpCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "HTTP");
        final TestCategoryDto annotationsCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Annotations");
        final TestQuestionDto createdQuestion = createQuestion(
                course.id(),
                ownerAuthentication.accessToken().value(),
                initialQuestionRequest(httpCategory.id())
        );

        final ResultActions updateResult = mockMvc.perform(put("/courses/{courseId}/questions/{questionId}", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedQuestionRequest(annotationsCategory.id()))));

        final TestQuestionDto updatedQuestion = readResponse(updateResult, TestQuestionDto.class);
        final ResultActions versionsResult = mockMvc.perform(get("/courses/{courseId}/questions/{questionId}/versions", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuestionVersionDto> versions = readVersionListResponse(versionsResult);

        itShouldReturnOkStatus(updateResult);
        itShouldReturnOkStatus(versionsResult);
        assertThat(updatedQuestion.id()).isEqualTo(createdQuestion.id());
        assertThat(updatedQuestion.currentVersionNumber()).isEqualTo(2);
        assertThat(updatedQuestion.createdAt()).isEqualTo(createdQuestion.createdAt());
        assertThat(updatedQuestion.updatedAt()).isAfterOrEqualTo(createdQuestion.updatedAt());
        assertThat(updatedQuestion.prompt()).isEqualTo("Which annotation is typically used to bind a method to GET requests?");
        assertThat(updatedQuestion.categories())
                .extracting(TestQuestionCategoryDto::name)
                .containsExactly("Annotations");
        assertThat(updatedQuestion.answers())
                .extracting(TestQuestionAnswerDto::content)
                .containsExactly("@GetMapping", "@Bean");

        assertThat(versions).hasSize(2);
        assertThat(versions.getFirst().versionNumber()).isEqualTo(2);
        assertThat(versions.get(0).prompt()).isEqualTo("Which annotation is typically used to bind a method to GET requests?");
        assertThat(versions.get(0).categories())
                .extracting(TestQuestionCategoryDto::name)
                .containsExactly("Annotations");
        assertThat(versions.get(0).answers())
                .extracting(TestQuestionAnswerDto::content)
                .containsExactly("@GetMapping", "@Bean");
        assertThat(versions.get(1).versionNumber()).isEqualTo(1);
        assertThat(versions.get(1).prompt()).isEqualTo("Which annotation is typically used to expose an HTTP endpoint class?");
        assertThat(versions.get(1).categories())
                .extracting(TestQuestionCategoryDto::name)
                .containsExactly("HTTP");
        assertThat(versions.get(1).answers())
                .extracting(TestQuestionAnswerDto::content)
                .containsExactly("@RestController", "@Repository");
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingQuestionAsRegularNonOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        final TestCategoryDto webCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Web");
        final TestQuestionDto createdQuestion = createQuestion(
                course.id(),
                ownerAuthentication.accessToken().value(),
                initialQuestionRequest(webCategory.id())
        );
        final String anotherUserAccessToken = authenticationApi.registerAndLogin().accessToken().value();

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}/questions/{questionId}", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(anotherUserAccessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedQuestionRequest(webCategory.id()))));

        itShouldReturnForbiddenStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingQuestionWithoutAnyChanges() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Security Associate",
                        "A focused course for filters, JWTs, authorization rules, and access control."
                )
        );
        final TestCategoryDto httpCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "HTTP");
        final TestQuestionDto createdQuestion = createQuestion(
                course.id(),
                ownerAuthentication.accessToken().value(),
                initialQuestionRequest(httpCategory.id())
        );

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}/questions/{questionId}", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialQuestionRequest(httpCategory.id()))));

        final TestRuntimeExceptionDto response = readResponse(result, TestRuntimeExceptionDto.class);

        itShouldReturnBadRequestStatus(result);
        assertThat(response).isEqualTo(new TestRuntimeExceptionDto(
                "IllegalArgumentException",
                "Question update must change the prompt, answers, or categories."
        ));

        final ResultActions versionsResult = mockMvc.perform(get("/courses/{courseId}/questions/{questionId}/versions", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuestionVersionDto> versions = readVersionListResponse(versionsResult);

        itShouldReturnOkStatus(versionsResult);
        assertThat(versions).hasSize(1);
        assertThat(versions.getFirst().versionNumber()).isEqualTo(1);
    }

    @Test
    void shouldCreateNewVersionWhenOnlyCorrectAnswerChanges() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Security Associate",
                        "A focused course for filters, JWTs, authorization rules, and access control."
                )
        );
        final TestCategoryDto httpCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "HTTP");
        final TestQuestionDto createdQuestion = createQuestion(
                course.id(),
                ownerAuthentication.accessToken().value(),
                initialQuestionRequest(httpCategory.id())
        );

        final ResultActions updateResult = mockMvc.perform(put("/courses/{courseId}/questions/{questionId}", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionRequestWithFlippedCorrectAnswer(httpCategory.id()))));

        final TestQuestionDto updatedQuestion = readResponse(updateResult, TestQuestionDto.class);
        final ResultActions versionsResult = mockMvc.perform(get("/courses/{courseId}/questions/{questionId}/versions", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuestionVersionDto> versions = readVersionListResponse(versionsResult);

        itShouldReturnOkStatus(updateResult);
        itShouldReturnOkStatus(versionsResult);
        assertThat(updatedQuestion.currentVersionNumber()).isEqualTo(2);
        assertThat(updatedQuestion.answers())
                .extracting(TestQuestionAnswerDto::correct)
                .containsExactly(false, true);
        assertThat(versions).hasSize(2);
        assertThat(versions.getFirst().answers())
                .extracting(TestQuestionAnswerDto::correct)
                .containsExactly(false, true);
        assertThat(versions.get(1).answers())
                .extracting(TestQuestionAnswerDto::correct)
                .containsExactly(true, false);
    }

    @Test
    void shouldReturnNotFoundWhenFetchingVersionsForMissingQuestion() throws Exception {
        final String accessToken = authenticationApi.loginAsDefaultAdmin().accessToken().value();
        final TestCourseDto course = createCourse(
                accessToken,
                new TestCreateCourseRequest(
                        "Docker For Java Engineers",
                        "A practical course about images, layers, compose flows, and runtime fundamentals."
                )
        );

        final ResultActions result = mockMvc.perform(get("/courses/{courseId}/questions/{questionId}/versions", course.id(), 999_999L)
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON));

        final TestRuntimeExceptionDto response = readResponse(result, TestRuntimeExceptionDto.class);

        itShouldReturnNotFoundStatus(result);
        assertThat(response).isEqualTo(new TestRuntimeExceptionDto(
                "QuestionNotFoundException",
                "Question with id 999999 was not found."
        ));
    }

    private TestCreateQuestionRequest initialQuestionRequest(final Long categoryId) {
        return new TestCreateQuestionRequest(
                "Which annotation is typically used to expose an HTTP endpoint class?",
                List.of(
                        new TestQuestionAnswerRequest("@RestController", true),
                        new TestQuestionAnswerRequest("@Repository", false)
                ),
                List.of(categoryId)
        );
    }

    private TestUpdateQuestionRequest updatedQuestionRequest(final Long categoryId) {
        return new TestUpdateQuestionRequest(
                "Which annotation is typically used to bind a method to GET requests?",
                List.of(
                        new TestQuestionAnswerRequest("@GetMapping", true),
                        new TestQuestionAnswerRequest("@Bean", false)
                ),
                List.of(categoryId)
        );
    }

    private TestUpdateQuestionRequest questionRequestWithFlippedCorrectAnswer(final Long categoryId) {
        return new TestUpdateQuestionRequest(
                "Which annotation is typically used to expose an HTTP endpoint class?",
                List.of(
                        new TestQuestionAnswerRequest("@RestController", false),
                        new TestQuestionAnswerRequest("@Repository", true)
                ),
                List.of(categoryId)
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

    private List<TestQuestionVersionDto> readVersionListResponse(
            final ResultActions result
    ) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, new TypeReference<>() {
        });
    }

    private String bearerToken(final String accessToken) {
        return BEARER_PREFIX + accessToken;
    }

    private <T> T readResponse(
            final ResultActions result,
            final Class<T> responseClass
    ) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, responseClass);
    }
}
