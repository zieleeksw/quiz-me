package pl.zieleeksw.quiz_me.course;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldHaveEmptyResponseBody;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnOkStatus;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.itShouldReturnUnauthorizedStatus;

class ShouldFetchCoursesIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldReturnUnauthorizedWhenFetchingCoursesWithoutToken() throws Exception {
        final ResultActions result = mockMvc.perform(get("/courses")
                .contentType(MediaType.APPLICATION_JSON));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldFetchAllCoursesForAuthenticatedUser() throws Exception {
        final var firstOwner = authenticationApi.registerAndLogin();
        final var secondOwner = authenticationApi.registerAndLogin();

        createCourse(
                firstOwner.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );
        createCourse(
                secondOwner.accessToken().value(),
                new TestCreateCourseRequest(
                        "Docker For Java Engineers",
                        "A practical course about images, networks, compose flows, and production runtime."
                )
        );

        final ResultActions result = mockMvc.perform(get("/courses")
                .header(AUTHORIZATION_HEADER, bearerToken(firstOwner.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestCourseDto> response = readListResponse(result);

        itShouldReturnOkStatus(result);
        assertThat(response).hasSize(2);
        assertThat(response)
                .extracting(TestCourseDto::name)
                .containsExactly(
                        "Docker For Java Engineers",
                        "Spring Boot Associate"
                );
        assertThat(response)
                .extracting(TestCourseDto::ownerUserId)
                .contains(secondOwner.user().id(), firstOwner.user().id());
        assertThat(response).allSatisfy(course -> {
            assertThat(course.questionCount()).isEqualTo(course.expectedQuestionCount());
            assertThat(course.quizCount()).isEqualTo(course.expectedQuizCount());
            assertThat(course.progressPercent()).isEqualTo(course.expectedProgressPercent());
        });
    }

    private void createCourse(
            final String accessToken,
            final TestCreateCourseRequest request
    ) throws Exception {
        mockMvc.perform(post("/courses")
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    private List<TestCourseDto> readListResponse(
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
