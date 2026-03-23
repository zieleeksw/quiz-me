package pl.zieleeksw.quiz_me.utils;

import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import pl.zieleeksw.quiz_me.TestFieldValidationErrorDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HttpValidationUtils {

    public static void itShouldReturnOkStatus(final ResultActions result) throws Exception {
        final ResultMatcher ok = status().isOk();
        result.andExpect(ok);
    }

    public static void itShouldReturnCreatedStatus(final ResultActions result) throws Exception {
        final ResultMatcher created = status().isCreated();
        result.andExpect(created);
    }

    public static void itShouldReturnBadRequestStatus(final ResultActions result) throws Exception {
        final ResultMatcher badRequest = status().isBadRequest();
        result.andExpect(badRequest);
    }

    public static void itShouldReturnUnauthorizedStatus(final ResultActions result) throws Exception {
        final ResultMatcher unauthorized = status().isUnauthorized();
        result.andExpect(unauthorized);
    }

    public static void itShouldReturnForbiddenStatus(final ResultActions result) throws Exception {
        final ResultMatcher forbidden = status().isForbidden();
        result.andExpect(forbidden);
    }

    public static void itShouldReturnConflictStatus(final ResultActions result) throws Exception {
        final ResultMatcher conflict = status().isConflict();
        result.andExpect(conflict);
    }

    public static void itShouldReturnNotFoundStatus(final ResultActions result) throws Exception {
        final ResultMatcher notFound = status().isNotFound();
        result.andExpect(notFound);
    }

    public static void itShouldHaveEmptyResponseBody(final ResultActions result) throws Exception {
        final String content = result.andReturn().getResponse().getContentAsString();
        assertThat(content).isEmpty();
    }

    public static void itShouldContainErrorValidationDto(
            final TestFieldValidationErrorDto response,
            final TestFieldValidationErrorDto expectedDto) {
        final List<TestFieldValidationErrorDto.TestFieldErrorDto> responseErrors = response
                .getErrors();

        assertThat(responseErrors).isNotNull();
        assertThat(responseErrors).isNotEmpty();

        final List<TestFieldValidationErrorDto.TestFieldErrorDto> expectedErrors = expectedDto
                .getErrors();

        assertThat(responseErrors).hasSameSizeAs(expectedErrors);

        for (final TestFieldValidationErrorDto.TestFieldErrorDto expectedError : expectedErrors) {
            final boolean containsError = responseErrors.stream()
                    .anyMatch(error -> error.field().equals(expectedError.field())
                            && error.message().equals(expectedError.message()));

            assertThat(containsError)
                    .as("Expected error for field '%s' with message '%s' not found",
                            expectedError.field(), expectedError.message())
                    .isTrue();
        }

        assertThat(response.getException())
                .isEqualTo(expectedDto.getException());
    }
}
