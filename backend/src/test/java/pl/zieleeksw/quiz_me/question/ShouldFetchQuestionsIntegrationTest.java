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
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;
import pl.zieleeksw.quiz_me.category.TestCategoryDto;
import pl.zieleeksw.quiz_me.category.TestCreateCategoryRequest;
import pl.zieleeksw.quiz_me.course.TestCourseDto;
import pl.zieleeksw.quiz_me.course.TestCreateCourseRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldHaveEmptyResponseBody;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnOkStatus;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnUnauthorizedStatus;

class ShouldFetchQuestionsIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldReturnUnauthorizedWhenFetchingQuestionsWithoutToken() throws Exception {
        final ResultActions result = mockMvc.perform(get("/courses/{courseId}/questions", 999L)
                .contentType(MediaType.APPLICATION_JSON));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldReturnUnauthorizedWhenFetchingQuestionPreviewWithoutToken() throws Exception {
        final ResultActions result = mockMvc.perform(get("/courses/{courseId}/questions/preview", 999L)
                .param("page", "0")
                .param("size", "5")
                .contentType(MediaType.APPLICATION_JSON));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldFetchCurrentQuestionStateAfterVersionedUpdate() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        final TestCategoryDto webCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Web");
        final TestCategoryDto serializationCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Serialization");
        final TestQuestionDto createdQuestion = createQuestion(course.id(), ownerAuthentication.accessToken().value(), initialQuestionRequest(webCategory.id()));

        updateQuestion(course.id(), createdQuestion.id(), ownerAuthentication.accessToken().value(), updatedQuestionRequest(serializationCategory.id()));

        final String learnerAccessToken = authenticationApi.registerAndLogin().accessToken().value();
        final ResultActions result = mockMvc.perform(get("/courses/{courseId}/questions", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(learnerAccessToken))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuestionDto> response = readListResponse(result);

        itShouldReturnOkStatus(result);
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(createdQuestion.id());
        assertThat(response.getFirst().currentVersionNumber()).isEqualTo(2);
        assertThat(response.getFirst().prompt()).isEqualTo("Which bean usually resolves outgoing JSON serialization in Spring MVC?");
        assertThat(response.getFirst().categories())
                .extracting(TestQuestionCategoryDto::name)
                .containsExactly("Serialization");
        assertThat(response.getFirst().answers())
                .extracting(TestQuestionAnswerDto::content)
                .containsExactly("HttpMessageConverter", "TaskExecutor");
        assertThat(response.getFirst().answers())
                .filteredOn(TestQuestionAnswerDto::correct)
                .hasSize(1)
                .first()
                .extracting(TestQuestionAnswerDto::content)
                .isEqualTo("HttpMessageConverter");
    }

    @Test
    void shouldHideArchivedCategoriesFromCurrentQuestionStateButKeepThemInVersionHistory() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        final TestCategoryDto webCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Web");
        final TestCategoryDto controllersCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Controllers");
        final TestQuestionDto createdQuestion = createQuestion(
                course.id(),
                ownerAuthentication.accessToken().value(),
                new TestCreateQuestionRequest(
                        "Which bean is responsible for handling incoming REST requests?",
                        List.of(
                                new TestQuestionAnswerRequest("DispatcherServlet", true),
                                new TestQuestionAnswerRequest("EntityManager", false)
                        ),
                        List.of(webCategory.id(), controllersCategory.id())
                )
        );

        final ResultActions archiveResult = mockMvc.perform(delete("/courses/{courseId}/categories/{categoryId}", course.id(), webCategory.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final ResultActions currentQuestionsResult = mockMvc.perform(get("/courses/{courseId}/questions", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuestionDto> currentQuestions = readListResponse(currentQuestionsResult);

        final ResultActions versionsResult = mockMvc.perform(get("/courses/{courseId}/questions/{questionId}/versions", course.id(), createdQuestion.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestQuestionVersionDto> versions = objectMapper.readValue(
                versionsResult.andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        itShouldReturnOkStatus(currentQuestionsResult);
        itShouldReturnOkStatus(versionsResult);
        assertThat(archiveResult.andReturn().getResponse().getStatus()).isEqualTo(204);
        assertThat(currentQuestions).hasSize(1);
        assertThat(currentQuestions.getFirst().categories())
                .extracting(TestQuestionCategoryDto::name)
                .containsExactly("Controllers");
        assertThat(versions).hasSize(1);
        assertThat(versions.getFirst().categories())
                .extracting(TestQuestionCategoryDto::name)
                .containsExactly("Web", "Controllers");
    }

    @Test
    void shouldFetchQuestionPreviewPageWithFiveItems() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        final TestCategoryDto webCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Web");

        for (int index = 1; index <= 6; index++) {
            createQuestion(
                    course.id(),
                    ownerAuthentication.accessToken().value(),
                    new TestCreateQuestionRequest(
                            "Preview question number " + index + " for pagination checks?",
                            List.of(
                                    new TestQuestionAnswerRequest("Correct answer " + index, true),
                                    new TestQuestionAnswerRequest("Wrong answer " + index, false)
                            ),
                            List.of(webCategory.id())
                    )
            );
        }

        final ResultActions result = mockMvc.perform(get("/courses/{courseId}/questions/preview", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .param("page", "0")
                .param("size", "5")
                .contentType(MediaType.APPLICATION_JSON));

        final TestQuestionPageDto response = readPageResponse(result);

        itShouldReturnOkStatus(result);
        assertThat(response.pageNumber()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(5);
        assertThat(response.totalItems()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
        assertThat(response.items()).hasSize(5);
        assertThat(response.items().getFirst().prompt()).isEqualTo("Preview question number 6 for pagination checks?");
        assertThat(response.items().getLast().prompt()).isEqualTo("Preview question number 2 for pagination checks?");
    }

    @Test
    void shouldFilterQuestionPreviewBySearchAndCategory() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        final TestCategoryDto webCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Web");
        final TestCategoryDto securityCategory = createCategory(course.id(), ownerAuthentication.accessToken().value(), "Security");

        createQuestion(
                course.id(),
                ownerAuthentication.accessToken().value(),
                new TestCreateQuestionRequest(
                        "Which bean handles incoming REST requests in the web stack?",
                        List.of(
                                new TestQuestionAnswerRequest("DispatcherServlet", true),
                                new TestQuestionAnswerRequest("EntityManager", false)
                        ),
                        List.of(webCategory.id())
                )
        );
        createQuestion(
                course.id(),
                ownerAuthentication.accessToken().value(),
                new TestCreateQuestionRequest(
                        "Which filter usually inspects the bearer token in a security chain?",
                        List.of(
                                new TestQuestionAnswerRequest("JwtAuthenticationFilter", true),
                                new TestQuestionAnswerRequest("DispatcherServlet", false)
                        ),
                        List.of(securityCategory.id())
                )
        );

        final ResultActions result = mockMvc.perform(get("/courses/{courseId}/questions/preview", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .param("page", "0")
                .param("size", "5")
                .param("search", "security")
                .param("categoryId", String.valueOf(securityCategory.id()))
                .contentType(MediaType.APPLICATION_JSON));

        final TestQuestionPageDto response = readPageResponse(result);

        itShouldReturnOkStatus(result);
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().categories())
                .extracting(TestQuestionCategoryDto::name)
                .containsExactly("Security");
        assertThat(response.items().getFirst().prompt()).contains("security chain");
    }

    private TestCreateQuestionRequest initialQuestionRequest(final Long categoryId) {
        return new TestCreateQuestionRequest(
                "Which bean is responsible for handling incoming REST requests?",
                List.of(
                        new TestQuestionAnswerRequest("DispatcherServlet", true),
                        new TestQuestionAnswerRequest("EntityManager", false)
                ),
                List.of(categoryId)
        );
    }

    private TestUpdateQuestionRequest updatedQuestionRequest(final Long categoryId) {
        return new TestUpdateQuestionRequest(
                "Which bean usually resolves outgoing JSON serialization in Spring MVC?",
                List.of(
                        new TestQuestionAnswerRequest("HttpMessageConverter", true),
                        new TestQuestionAnswerRequest("TaskExecutor", false)
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

    private void updateQuestion(
            final Long courseId,
            final Long questionId,
            final String accessToken,
            final TestUpdateQuestionRequest request
    ) throws Exception {
        mockMvc.perform(put("/courses/{courseId}/questions/{questionId}", courseId, questionId)
                        .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    private List<TestQuestionDto> readListResponse(
            final ResultActions result
    ) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, new TypeReference<>() {
        });
    }

    private TestQuestionPageDto readPageResponse(
            final ResultActions result
    ) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, TestQuestionPageDto.class);
    }

    private String bearerToken(final String accessToken) {
        return BEARER_PREFIX + accessToken;
    }
}
