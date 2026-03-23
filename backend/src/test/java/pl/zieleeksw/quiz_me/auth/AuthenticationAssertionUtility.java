package pl.zieleeksw.quiz_me.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.zieleeksw.quiz_me.user.TestUserDto;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationAssertionUtility {

    static void itShouldContainValidAuthenticationStructure(
            final ObjectMapper objectMapper,
            final String jsonContent) throws Exception {
        final JsonNode root = objectMapper.readTree(jsonContent);

        final List<String> rootFields = new ArrayList<>();
        root.fieldNames().forEachRemaining(rootFields::add);

        assertThat(rootFields)
                .containsOnly("user", "accessToken", "refreshToken");

        final JsonNode userNode = root.get("user");
        assertThat(userNode).isNotNull();

        final List<String> userFields = new ArrayList<>();
        userNode.fieldNames().forEachRemaining(userFields::add);

        assertThat(userFields)
                .containsOnly("id", "email", "roleId");

        final JsonNode accessTokenNode = root.get("accessToken");
        assertThat(accessTokenNode).isNotNull();

        final List<String> accessTokenFields = new ArrayList<>();
        accessTokenNode.fieldNames().forEachRemaining(accessTokenFields::add);

        assertThat(accessTokenFields)
                .containsOnly("value");

        final JsonNode refreshTokenNode = root.get("refreshToken");
        assertThat(refreshTokenNode).isNotNull();

        final List<String> refreshTokenFields = new ArrayList<>();
        refreshTokenNode.fieldNames().forEachRemaining(refreshTokenFields::add);

        assertThat(refreshTokenFields)
                .containsOnly("value");
    }

    static void itShouldContainValidAuthenticationDto(
            final TestAuthenticationDto response,
            final TestUserDto registeredUser) {
        assertThat(response).isNotNull();

        itShouldContainNonEmptyToken(response.accessToken());
        itShouldContainNonEmptyToken(response.refreshToken());
        itShouldContainValidUserDto(response.user(), registeredUser);
    }

    private static void itShouldContainNonEmptyToken(final TestJwtDto token) {
        assertThat(token).isNotNull();
        assertThat(token.value()).isNotBlank();
    }

    private static void itShouldContainValidUserDto(
            final TestUserDto responseUser,
            final TestUserDto expectedUser) {
        assertThat(responseUser).isNotNull();
        assertThat(responseUser.id()).isEqualTo(expectedUser.id());
        assertThat(responseUser.email()).isEqualTo(expectedUser.email());
        assertThat(responseUser.roleId()).isEqualTo(expectedUser.roleId());
    }
}
