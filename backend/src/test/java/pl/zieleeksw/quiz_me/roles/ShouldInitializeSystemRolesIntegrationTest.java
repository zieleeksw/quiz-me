package pl.zieleeksw.quiz_me.roles;

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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldInitializeSystemRolesIntegrationTest extends BaseIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldFetchRolesWhenUserIsAdmin() throws Exception {
        final var authentication = authenticationApi.loginAsDefaultAdmin();
        final String accessToken = authentication.accessToken().value();

        final ResultActions result = mockMvc.perform(get("/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));

        itShouldReturnOkStatus(result);

        final List<TestRoleDto> roles = readResponse(result, new TypeReference<>() {
        });

        itShouldAllocateIdForEachRole(roles);
        itShouldHaveSameSizeAndContentAsRolesDefinedInEnum(roles);
    }

    @Test
    void shouldNotFetchRolesWhenUserIsNotAdmin() throws Exception {
        final var authentication = authenticationApi.registerAndLogin();
        final String accessToken = authentication.accessToken().value();

        final ResultActions result = mockMvc.perform(get("/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));

        itShouldReturnForbiddenStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    @Test
    void shouldNotFetchRolesWhenNoUserIsLoggedIn() throws Exception {
        final ResultActions result = mockMvc.perform(get("/roles")
                .contentType(MediaType.APPLICATION_JSON));

        itShouldReturnUnauthorizedStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    private static void itShouldAllocateIdForEachRole(final List<TestRoleDto> roles) {
        roles.forEach(role -> {
            final Long id = role.id();

            assertThat(id).isNotNull();
            assertThat(id).isGreaterThan(-1);
        });
    }

    private static void itShouldHaveSameSizeAndContentAsRolesDefinedInEnum(final List<TestRoleDto> fetchedRoles) {
        final List<String> retrievedRoles = fetchedRoles.stream()
                .map(TestRoleDto::name)
                .toList();

        final List<String> expectedRoles = Arrays.stream(TestRole.values())
                .map(Enum::name)
                .toList();

        assertThat(retrievedRoles)
                .hasSize(expectedRoles.size())
                .containsExactlyInAnyOrderElementsOf(expectedRoles);
    }

    private <T> T readResponse(final ResultActions result, final TypeReference<T> responseType) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, responseType);
    }
}
