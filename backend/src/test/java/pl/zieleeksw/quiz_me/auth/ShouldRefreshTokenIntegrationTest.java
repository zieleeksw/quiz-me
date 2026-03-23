package pl.zieleeksw.quiz_me.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.zieleeksw.quiz_me.BaseIntegration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static pl.zieleeksw.quiz_me.auth.AuthenticationAssertionUtility.itShouldContainValidAuthenticationDto;
import static pl.zieleeksw.quiz_me.auth.AuthenticationAssertionUtility.itShouldContainValidAuthenticationStructure;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@TestPropertySource(properties = {
        "app.security.jwt.expiration.refresh-token-ms=1000"
})
class ShouldRefreshTokenIntegrationTest extends BaseIntegration {

    @Autowired
    private AuthenticationApi authenticationApi;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRefreshAccessTokenWhenValidRefreshTokenProvided() throws Exception {
        final TestAuthenticationDto loggedUser = authenticationApi.registerAndLogin();
        final String refreshToken = loggedUser.refreshToken().value();

        final var refreshRequest = new TestRefreshTokenRequest(refreshToken);

        final ResultActions refreshResult = mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)));

        final MvcResult mvcResult = refreshResult.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();

        itShouldContainValidAuthenticationStructure(objectMapper, content);
        itShouldReturnOkStatus(refreshResult);

        final TestAuthenticationDto refreshResponse = readResponse(refreshResult, TestAuthenticationDto.class);
        itShouldContainValidAuthenticationDto(refreshResponse, loggedUser.user());
        itShouldContainDifferentAccessToken(refreshResponse, loggedUser);
        itShouldContainSameRefreshToken(refreshResponse, loggedUser);
    }

    @Test
    void shouldFailWhenInvalidRefreshTokenProvided() throws Exception {
        final TestRefreshTokenRequest invalidRefreshToken = new TestRefreshTokenRequest("invalid_refresh_token");

        final ResultActions result = mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRefreshToken)));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldFailWhenRefreshTokenIsExpired() throws Exception {
        final TestAuthenticationDto loggedUser = authenticationApi.registerAndLogin();

        Thread.sleep(1100);

        final var refreshRequest = new TestRefreshTokenRequest(loggedUser.refreshToken().value());
        final ResultActions result = mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)));

        itShouldReturnUnauthorizedStatus(result);
    }

    private <T> T readResponse(final ResultActions result, final Class<T> responseClass) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, responseClass);
    }

    private static void itShouldContainDifferentAccessToken(
            final TestAuthenticationDto refreshed,
            final TestAuthenticationDto original) {
        assertThat(refreshed.accessToken().value()).isNotEqualTo(original.accessToken().value());
    }

    private static void itShouldContainSameRefreshToken(
            final TestAuthenticationDto refreshed,
            final TestAuthenticationDto original) {
        assertThat(refreshed.refreshToken().value()).isEqualTo(original.refreshToken().value());
    }
}
