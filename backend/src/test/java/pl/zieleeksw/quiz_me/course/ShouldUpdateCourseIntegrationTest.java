package pl.zieleeksw.quiz_me.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.TestFieldValidationErrorDto;
import pl.zieleeksw.quiz_me.TestRuntimeExceptionDto;
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldUpdateCourseIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldUpdateOwnCourseWhenAuthenticatedAsOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final String accessToken = ownerAuthentication.accessToken().value();
        final TestCourseDto createdCourse = createCourse(
                accessToken,
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );

        final TestUpdateCourseRequest request = new TestUpdateCourseRequest(
                "Spring Boot Professional",
                "An advanced course covering architecture, observability, testing, and deployment."
        );

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}", createdCourse.id())
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        final TestCourseDto response = readResponse(result, TestCourseDto.class);

        itShouldReturnOkStatus(result);
        assertThat(response.id()).isEqualTo(createdCourse.id());
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.description()).isEqualTo(request.description());
        assertThat(response.createdAt()).isEqualTo(createdCourse.createdAt());
        assertThat(response.ownerUserId()).isEqualTo(ownerAuthentication.user().id());
        assertThat(response.questionCount()).isEqualTo(createdCourse.questionCount());
        assertThat(response.quizCount()).isEqualTo(createdCourse.quizCount());
        assertThat(response.progressPercent()).isEqualTo(createdCourse.progressPercent());
    }

    @Test
    void shouldUpdateCourseWhenAuthenticatedAsAdmin() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto createdCourse = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        final String adminAccessToken = authenticationApi.loginAsDefaultAdmin().accessToken().value();

        final TestUpdateCourseRequest request = new TestUpdateCourseRequest(
                "Spring Boot Professional",
                "An advanced course covering architecture, observability, testing, and deployment."
        );

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}", createdCourse.id())
                .header(AUTHORIZATION_HEADER, bearerToken(adminAccessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        final TestCourseDto response = readResponse(result, TestCourseDto.class);

        itShouldReturnOkStatus(result);
        assertThat(response.id()).isEqualTo(createdCourse.id());
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.description()).isEqualTo(request.description());
        assertThat(response.createdAt()).isEqualTo(createdCourse.createdAt());
        assertThat(response.ownerUserId()).isEqualTo(ownerAuthentication.user().id());
        assertThat(response.questionCount()).isEqualTo(createdCourse.questionCount());
        assertThat(response.quizCount()).isEqualTo(createdCourse.quizCount());
        assertThat(response.progressPercent()).isEqualTo(createdCourse.progressPercent());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingCourseWithInvalidPayload() throws Exception {
        final String accessToken = authenticationApi.loginAsDefaultAdmin().accessToken().value();
        final TestCourseDto createdCourse = createCourse(
                accessToken,
                new TestCreateCourseRequest(
                        "Spring Security Associate",
                        "A focused course for filters, tokens, and access control."
                )
        );

        final TestUpdateCourseRequest request = new TestUpdateCourseRequest(
                "AB",
                "Still valid description for the update path."
        );

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}", createdCourse.id())
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        final TestFieldValidationErrorDto response = readResponse(result, TestFieldValidationErrorDto.class);

        itShouldReturnBadRequestStatus(result);
        itShouldContainErrorValidationDto(
                response,
                new TestFieldValidationErrorDto(
                        "MethodArgumentNotValidException",
                        java.util.List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                "name",
                                "Course name is too short. Min length is 3 characters."
                        ))
                )
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenUpdatingCourseWithoutToken() throws Exception {
        final TestUpdateCourseRequest request = new TestUpdateCourseRequest(
                "Updated Name",
                "A valid updated description for the unauthorized test path."
        );

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingCourseAsRegularUser() throws Exception {
        final String ownerAccessToken = authenticationApi.registerAndLogin().accessToken().value();
        final TestCourseDto createdCourse = createCourse(
                ownerAccessToken,
                new TestCreateCourseRequest(
                        "Docker For Java Engineers",
                        "A practical course about images, networks, compose flows, and production runtime."
                )
        );
        final String userAccessToken = authenticationApi.registerAndLogin().accessToken().value();
        final TestUpdateCourseRequest request = new TestUpdateCourseRequest(
                "Docker For Enterprise Java",
                "A valid update body prepared by a non-admin user."
        );

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}", createdCourse.id())
                .header(AUTHORIZATION_HEADER, bearerToken(userAccessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        itShouldReturnForbiddenStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingCourse() throws Exception {
        final String accessToken = authenticationApi.loginAsDefaultAdmin().accessToken().value();
        final TestUpdateCourseRequest request = new TestUpdateCourseRequest(
                "Ghost Course",
                "A valid payload that targets a course identifier that does not exist."
        );

        final ResultActions result = mockMvc.perform(put("/courses/{courseId}", 999_999L)
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        final TestRuntimeExceptionDto response = readResponse(result, TestRuntimeExceptionDto.class);

        itShouldReturnNotFoundStatus(result);
        assertThat(response).isEqualTo(new TestRuntimeExceptionDto(
                "CourseNotFoundException",
                "Course with id 999999 was not found."
        ));
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
