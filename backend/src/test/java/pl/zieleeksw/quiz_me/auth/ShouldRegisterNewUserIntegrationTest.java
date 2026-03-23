package pl.zieleeksw.quiz_me.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.TestFieldValidationErrorDto;
import pl.zieleeksw.quiz_me.TestFieldValidationErrorDto.TestFieldErrorDto;
import pl.zieleeksw.quiz_me.TestRuntimeExceptionDto;
import pl.zieleeksw.quiz_me.user.TestUserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ShouldRegisterNewUserIntegrationTest extends BaseIntegration {

    private static final String RESPONSE_EMAIL_FIELD_NAME = "email";

    private static final String RESPONSE_PASSWORD_FIELD_NAME = "password";

    private static final String VALID_PASSWORD = "password12345678";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Stream<Arguments> provideArguments() {
        final String validEmail = "abstract.simon.code@gmail.com";
        final String anotherValidEmail = "java.is.love@cloud.it";

        final String tooLongEmail = "a".repeat(256);
        final String tooShortEmail = "a".repeat(5);
        final String blankEmail = "";
        final String noAtSymbol = "userdomain.com";
        final String consecutiveDots = "user@domain..com";
        final String hyphenStartDomain = "user@-domain.com";
        final String expectedException = "MethodArgumentNotValidException";

        final String blankPassword = "";
        final String tooLongPassword = "a".repeat(129);
        final String tooShortPassword = "a".repeat(11);

        final String blankOrNullEmailFieldValidationMessageResponse = "Email address cannot be empty.";
        final List<TestFieldErrorDto> emailBlankErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_EMAIL_FIELD_NAME,
                        blankOrNullEmailFieldValidationMessageResponse));

        final List<TestFieldErrorDto> emailNullErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_EMAIL_FIELD_NAME,
                        blankOrNullEmailFieldValidationMessageResponse));

        final String maxLenMsgEmailFieldValidationMessageResponse = "Email is too long. Max length is 255 characters.";
        final List<TestFieldErrorDto> emailTooLongErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_EMAIL_FIELD_NAME,
                        maxLenMsgEmailFieldValidationMessageResponse));

        final String minLenMsgEmailFieldValidationMessageResponse = "Email is too short. Min length is 11 characters.";

        final List<TestFieldErrorDto> emailTooShortErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_EMAIL_FIELD_NAME,
                        minLenMsgEmailFieldValidationMessageResponse));

        final String prefixErrorMsg = "Email is invalid: ";

        final List<TestFieldErrorDto> emailNoAtSymbolErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_EMAIL_FIELD_NAME,
                        prefixErrorMsg + noAtSymbol));

        final List<TestFieldErrorDto> emailConsecutiveDotsErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_EMAIL_FIELD_NAME,
                        prefixErrorMsg + consecutiveDots));

        final List<TestFieldErrorDto> emailHyphenStartDomainErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_EMAIL_FIELD_NAME,
                        prefixErrorMsg + hyphenStartDomain));

        final String blankOrNullPasswordFieldValidationMessageResponse = "Password cannot be empty.";
        final List<TestFieldErrorDto> passwordBlankErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_PASSWORD_FIELD_NAME,
                        blankOrNullPasswordFieldValidationMessageResponse));

        final String tooShortPasswordFieldValidationMessageResponse = "Password is too short. Min length is 12 characters.";
        final List<TestFieldErrorDto> passwordTooShortErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_PASSWORD_FIELD_NAME,
                        tooShortPasswordFieldValidationMessageResponse));

        final String tooLongPasswordFieldValidationMessageResponse = "Password is too long. Max length is 128 characters.";
        final List<TestFieldErrorDto> passwordTooLongErrors = List.of(
                new TestFieldErrorDto(
                        RESPONSE_PASSWORD_FIELD_NAME,
                        tooLongPasswordFieldValidationMessageResponse));

        final TestFieldValidationErrorDto emailBlankDto = new TestFieldValidationErrorDto(expectedException,
                emailBlankErrors);
        final TestFieldValidationErrorDto emailNullDto = new TestFieldValidationErrorDto(expectedException,
                emailNullErrors);
        final TestFieldValidationErrorDto emailTooLongDto = new TestFieldValidationErrorDto(expectedException,
                emailTooLongErrors);
        final TestFieldValidationErrorDto emailTooShortDto = new TestFieldValidationErrorDto(expectedException,
                emailTooShortErrors);
        final TestFieldValidationErrorDto emailNoAtSymbolDto = new TestFieldValidationErrorDto(
                expectedException, emailNoAtSymbolErrors);
        final TestFieldValidationErrorDto emailConsecutiveDotsDto = new TestFieldValidationErrorDto(
                expectedException, emailConsecutiveDotsErrors);
        final TestFieldValidationErrorDto emailHyphenStartDomainDto = new TestFieldValidationErrorDto(
                expectedException, emailHyphenStartDomainErrors);

        final TestFieldValidationErrorDto passwordBlankDto = new TestFieldValidationErrorDto(expectedException,
                passwordBlankErrors);
        final TestFieldValidationErrorDto passwordTooShortDto = new TestFieldValidationErrorDto(
                expectedException, passwordTooShortErrors);
        final TestFieldValidationErrorDto passwordTooLongDto = new TestFieldValidationErrorDto(
                expectedException, passwordTooLongErrors);

        return Stream.of(
                Arguments.of(tooLongEmail, VALID_PASSWORD, emailTooLongDto),
                Arguments.of(tooShortEmail, VALID_PASSWORD, emailTooShortDto),
                Arguments.of(blankEmail, VALID_PASSWORD, emailBlankDto),
                Arguments.of(null, VALID_PASSWORD, emailNullDto),
                Arguments.of(noAtSymbol, VALID_PASSWORD, emailNoAtSymbolDto),
                Arguments.of(consecutiveDots, VALID_PASSWORD, emailConsecutiveDotsDto),
                Arguments.of(hyphenStartDomain, VALID_PASSWORD, emailHyphenStartDomainDto),

                Arguments.of(validEmail, null, passwordBlankDto),
                Arguments.of(validEmail, blankPassword, passwordBlankDto),
                Arguments.of(validEmail, tooShortPassword, passwordTooShortDto),
                Arguments.of(validEmail, tooLongPassword, passwordTooLongDto),

                Arguments.of(validEmail, VALID_PASSWORD, null),
                Arguments.of(anotherValidEmail, VALID_PASSWORD, null));
        }

    private static void itShouldContainRuntimeExceptionDto(
            final TestRuntimeExceptionDto response,
            final TestRuntimeExceptionDto expectedErrorDto) {

        assertThat(response).isNotNull();
        assertThat(response.exception()).isNotNull();
        assertThat(response.message()).isNotNull();

        assertThat(response).isEqualTo(expectedErrorDto);
    }

    private static void itShouldAllocateNewId(final TestUserDto response) {
        final Long id = response.id();

        assertThat(id).isNotNull();
        assertThat(id).isGreaterThan(-1);
    }

    private static void itShouldContainEmailEqualTo(final TestUserDto response, final String email) {
        final String responseMail = response.email();

        assertThat(responseMail).isNotNull();
        assertThat(responseMail).isEqualTo(email);
    }

    @Test
    void shouldRegisterNewUserWhenEmailAlreadyExists() throws Exception {
        final String validEmail = "abstract.simon.code.valid@gmail.com";
        final TestRegisterUserRequest request = new TestRegisterUserRequest(validEmail, VALID_PASSWORD);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(request)));

        final ResultActions result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(request)));

        final TestRuntimeExceptionDto response = readResponse(result, TestRuntimeExceptionDto.class);

        itShouldReturnConflictStatus(result);

        final String expectedException = "EmailAlreadyExistsException";
        final String expectedMessage = String.format("User with email %s already exists.", validEmail);

        final TestRuntimeExceptionDto expectedErrorDto = new TestRuntimeExceptionDto(
                expectedException,
                expectedMessage);

        itShouldContainRuntimeExceptionDto(response, expectedErrorDto);
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void shouldRegisterNewUser(
            final String email,
            final String password,
            final TestFieldValidationErrorDto expectedErrorDto) throws Exception {
        final TestRegisterUserRequest request = new TestRegisterUserRequest(email, password);

        final ResultActions result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(request)));

        if (Objects.isNull(expectedErrorDto)) {
            final TestUserDto response = readResponse(result, TestUserDto.class);

            itShouldReturnCreatedStatus(result);
            itShouldAllocateNewId(response);
            itShouldContainEmailEqualTo(response, request.email());
            itShouldContainRoleIdAllocated(response);
        } else {
            final TestFieldValidationErrorDto response = readResponse(result, TestFieldValidationErrorDto.class);

            itShouldReturnBadRequestStatus(result);
            itShouldContainErrorValidationDto(response, expectedErrorDto);
        }
    }

    private void itShouldContainRoleIdAllocated(final TestUserDto response) {
        final Long id = response.roleId();

        assertThat(id).isNotNull();
        assertThat(id).isGreaterThan(-1);
    }

    private <T> T readResponse(final ResultActions result, final Class<T> responseClass) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, responseClass);
    }
}
