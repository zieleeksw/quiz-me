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
import pl.zieleeksw.quiz_me.course.TestCourseDto;
import pl.zieleeksw.quiz_me.course.TestCreateCourseRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void shouldFetchCurrentQuestionStateAfterVersionedUpdate() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        final TestQuestionDto createdQuestion = createQuestion(course.id(), ownerAuthentication.accessToken().value(), initialQuestionRequest());

        updateQuestion(course.id(), createdQuestion.id(), ownerAuthentication.accessToken().value(), updatedQuestionRequest());

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

    private TestCreateQuestionRequest initialQuestionRequest() {
        return new TestCreateQuestionRequest(
                "Which bean is responsible for handling incoming REST requests?",
                List.of(
                        new TestQuestionAnswerRequest("DispatcherServlet", true),
                        new TestQuestionAnswerRequest("EntityManager", false)
                )
        );
    }

    private TestUpdateQuestionRequest updatedQuestionRequest() {
        return new TestUpdateQuestionRequest(
                "Which bean usually resolves outgoing JSON serialization in Spring MVC?",
                List.of(
                        new TestQuestionAnswerRequest("HttpMessageConverter", true),
                        new TestQuestionAnswerRequest("TaskExecutor", false)
                )
        );
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

    private String bearerToken(final String accessToken) {
        return BEARER_PREFIX + accessToken;
    }
}
