package pl.zieleeksw.quiz_me.user;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "app.admin.email=test-admin@quiz.com",
        "app.admin.password=TestAdmin123!"
})
class ShouldInitializeAdminWithPropertiesIntegrationTest extends BaseIntegration {

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldCreateAdminWhenPropertiesAreDefined() throws Exception {
        // given - admin is created on application start with test properties
        final String adminEmail = "test-admin@quiz.com";
        final String adminPassword = "TestAdmin123!";

        // when
        final var authentication = authenticationApi.login(adminEmail, adminPassword);

        // then
        assertThat(authentication).isNotNull();
        assertThat(authentication.accessToken()).isNotNull();
        assertThat(authentication.accessToken().value()).isNotEmpty();
        assertThat(authentication.refreshToken()).isNotNull();
        assertThat(authentication.refreshToken().value()).isNotEmpty();
    }
}
