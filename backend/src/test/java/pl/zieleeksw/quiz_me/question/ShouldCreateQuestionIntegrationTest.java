package pl.zieleeksw.quiz_me.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.TestFieldValidationErrorDto;
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;
import pl.zieleeksw.quiz_me.course.TestCourseDto;
import pl.zieleeksw.quiz_me.course.TestCreateCourseRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldCreateQuestionIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldReturnUnauthorizedWhenCreatingQuestionWithoutToken() throws Exception {
        final TestCreateQuestionRequest request = validQuestionRequest();

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/questions", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldCreateQuestionAsCourseOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/questions", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validQuestionRequest())));

        final TestQuestionDto response = readResponse(result, TestQuestionDto.class);

        itShouldReturnCreatedStatus(result);
        assertThat(response.id()).isNotNull();
        assertThat(response.courseId()).isEqualTo(course.id());
        assertThat(response.currentVersionNumber()).isEqualTo(1);
        assertThat(response.prompt()).isEqualTo("Which bean is responsible for handling incoming REST requests?");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isEqualTo(response.createdAt());
        assertThat(response.answers()).hasSize(2);
        assertThat(response.answers())
                .extracting(TestQuestionAnswerDto::content)
                .containsExactly("DispatcherServlet", "EntityManager");
        assertThat(response.answers())
                .filteredOn(TestQuestionAnswerDto::correct)
                .hasSize(1)
                .first()
                .extracting(TestQuestionAnswerDto::content)
                .isEqualTo("DispatcherServlet");
    }

    @Test
    void shouldReturnForbiddenWhenCreatingQuestionAsRegularNonOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Security Deep Dive",
                        "A focused course for filters, JWTs, and authorization workflows."
                )
        );
        final String anotherUserAccessToken = authenticationApi.registerAndLogin().accessToken().value();

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/questions", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(anotherUserAccessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validQuestionRequest())));

        itShouldReturnForbiddenStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldReturnBadRequestWhenCreatingQuestionWithInvalidAnswers() throws Exception {
        final String accessToken = authenticationApi.loginAsDefaultAdmin().accessToken().value();
        final TestCourseDto course = createCourse(
                accessToken,
                new TestCreateCourseRequest(
                        "Docker For Java Engineers",
                        "A practical course about images, layers, compose flows, and runtime fundamentals."
                )
        );
        final TestCreateQuestionRequest request = new TestCreateQuestionRequest(
                "What header typically carries the bearer token for an API call?",
                List.of(
                        new TestQuestionAnswerRequest("Authorization", true),
                        new TestQuestionAnswerRequest("Cookie", true)
                )
        );

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/questions", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        final TestFieldValidationErrorDto response = readResponse(result, TestFieldValidationErrorDto.class);

        itShouldReturnBadRequestStatus(result);
        itShouldContainErrorValidationDto(
                response,
                new TestFieldValidationErrorDto(
                        "MethodArgumentNotValidException",
                        List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                "answers",
                                "Question must contain exactly 1 correct answer."
                        ))
                )
        );
    }

    private TestCreateQuestionRequest validQuestionRequest() {
        return new TestCreateQuestionRequest(
                "Which bean is responsible for handling incoming REST requests?",
                List.of(
                        new TestQuestionAnswerRequest("DispatcherServlet", true),
                        new TestQuestionAnswerRequest("EntityManager", false)
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
