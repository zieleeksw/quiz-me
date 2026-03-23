package pl.zieleeksw.quiz_me.auth;

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
import pl.zieleeksw.quiz_me.user.TestUserDto;

import java.util.stream.Stream;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static pl.zieleeksw.quiz_me.auth.AuthenticationAssertionUtility.itShouldContainValidAuthenticationDto;
import static pl.zieleeksw.quiz_me.auth.AuthenticationAssertionUtility.itShouldContainValidAuthenticationStructure;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldLoginUserIntegrationTest extends BaseIntegration {

    @Autowired
    private AuthenticationApi authenticationApi;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Stream<Arguments> provideArguments() {
        final String firstUserEmail = RandomEmailGenerator.generate();
        final String firstUserPassword = RandomPasswordGenerator.generate();

        final String secondUserEmail = RandomEmailGenerator.generate();
        final String secondUserPassword = RandomPasswordGenerator.generate();

        return Stream.of(
                Arguments.of(firstUserEmail, firstUserPassword),
                Arguments.of(secondUserEmail, secondUserPassword));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void shouldLoginOnValidCredentials(final String email, final String password) throws Exception {
        final TestUserDto registeredUser = authenticationApi.register(email, password);
        final TestLoginRequest request = new TestLoginRequest(registeredUser.email(), password);

        final ResultActions result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        itShouldReturnOkStatus(result);

        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        itShouldContainValidAuthenticationStructure(objectMapper, content);

        final TestAuthenticationDto response = readResponse(result, TestAuthenticationDto.class);
        itShouldContainValidAuthenticationDto(response, registeredUser);
    }

    @Test
    void shouldFailLoginOnInvalidCredentialsOnExistingUser() throws Exception {
        final String validEmail = RandomEmailGenerator.generate();
        final String validPassword = RandomPasswordGenerator.generate();
        final TestUserDto registeredUser = authenticationApi.register(validEmail, validPassword);

        final String invalidPassword = "wrongPassword123";
        final TestLoginRequest invalidRequest = new TestLoginRequest(registeredUser.email(), invalidPassword);

        final ResultActions result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldFailLoginOnNotExistingUser() throws Exception {
        final String notExistingEmail = RandomEmailGenerator.generate();
        final String notExistingPassword = RandomPasswordGenerator.generate();

        final TestLoginRequest request = new TestLoginRequest(notExistingEmail, notExistingPassword);

        final ResultActions result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    private <T> T readResponse(final ResultActions result, final Class<T> responseClass) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, responseClass);
    }
}