package pl.zieleeksw.quiz_me.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.TestFieldValidationErrorDto;
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldCreateCourseIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String NAME_FIELD = "name";
    private static final String DESCRIPTION_FIELD = "description";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    private static Stream<Arguments> provideArguments() {
        final String validName = "Spring Boot Associate";
        final String validDescription = "A focused course for architecture, persistence, and testing drills.";
        final String tooShortName = "AB";
        final String tooLongName = "a".repeat(121);
        final String blankName = "";
        final String tooShortDescription = "Too short";
        final String tooLongDescription = "a".repeat(1001);
        final String blankDescription = "";
        final String expectedException = "MethodArgumentNotValidException";

        return Stream.of(
                Arguments.of(
                        tooShortName,
                        validDescription,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        NAME_FIELD,
                                        "Course name is too short. Min length is 3 characters."
                                ))
                        )
                ),
                Arguments.of(
                        tooLongName,
                        validDescription,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        NAME_FIELD,
                                        "Course name is too long. Max length is 120 characters."
                                ))
                        )
                ),
                Arguments.of(
                        blankName,
                        validDescription,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        NAME_FIELD,
                                        "Course name cannot be empty."
                                ))
                        )
                ),
                Arguments.of(
                        null,
                        validDescription,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        NAME_FIELD,
                                        "Course name cannot be empty."
                                ))
                        )
                ),
                Arguments.of(
                        validName,
                        tooShortDescription,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        DESCRIPTION_FIELD,
                                        "Course description is too short. Min length is 10 characters."
                                ))
                        )
                ),
                Arguments.of(
                        validName,
                        tooLongDescription,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        DESCRIPTION_FIELD,
                                        "Course description is too long. Max length is 1000 characters."
                                ))
                        )
                ),
                Arguments.of(
                        validName,
                        blankDescription,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        DESCRIPTION_FIELD,
                                        "Course description cannot be empty."
                                ))
                        )
                ),
                Arguments.of(
                        validName,
                        null,
                        new TestFieldValidationErrorDto(
                                expectedException,
                                List.of(new TestFieldValidationErrorDto.TestFieldErrorDto(
                                        DESCRIPTION_FIELD,
                                        "Course description cannot be empty."
                                ))
                        )
                ),
                Arguments.of(validName, validDescription, null)
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingCourseWithoutToken() throws Exception {
        final TestCreateCourseRequest request = new TestCreateCourseRequest(
                "Spring Security Associate",
                "A course focused on authorization, JWTs, and filter chains."
        );

        final ResultActions result = mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldCreateCourseAsRegularUserAndAssignOwnership() throws Exception {
        final var authentication = authenticationApi.registerAndLogin();
        final String accessToken = authentication.accessToken().value();
        final TestCreateCourseRequest request = new TestCreateCourseRequest(
                "Docker For Java Engineers",
                "A course focused on images, networking, and production runtime patterns."
        );

        final ResultActions result = mockMvc.perform(post("/courses")
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        final TestCourseDto response = readResponse(result, TestCourseDto.class);

        itShouldReturnCreatedStatus(result);
        assertThat(response.ownerUserId()).isEqualTo(authentication.user().id());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void shouldCreateCourse(
            final String name,
            final String description,
            final TestFieldValidationErrorDto expectedErrorDto
    ) throws Exception {
        final String accessToken = authenticationApi.loginAsDefaultAdmin().accessToken().value();
        final TestCreateCourseRequest request = new TestCreateCourseRequest(name, description);

        final ResultActions result = mockMvc.perform(post("/courses")
                .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        if (Objects.isNull(expectedErrorDto)) {
            final TestCourseDto response = readResponse(result, TestCourseDto.class);

            itShouldReturnCreatedStatus(result);
            assertThat(response.id()).isNotNull().isGreaterThan(0L);
            assertThat(response.name()).isEqualTo(name.trim());
            assertThat(response.description()).isEqualTo(description.trim());
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.ownerUserId()).isNotNull().isGreaterThan(0L);
            assertThat(response.questionCount()).isEqualTo(response.expectedQuestionCount());
            assertThat(response.quizCount()).isEqualTo(response.expectedQuizCount());
            assertThat(response.progressPercent()).isEqualTo(response.expectedProgressPercent());
        } else {
            final TestFieldValidationErrorDto response = readResponse(result, TestFieldValidationErrorDto.class);

            itShouldReturnBadRequestStatus(result);
            itShouldContainErrorValidationDto(response, expectedErrorDto);
        }
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
