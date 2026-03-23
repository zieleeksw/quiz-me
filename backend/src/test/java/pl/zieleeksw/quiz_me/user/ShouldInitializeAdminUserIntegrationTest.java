package pl.zieleeksw.quiz_me.user;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldInitializeAdminUserIntegrationTest extends BaseIntegration {

    @Autowired
    private AuthenticationApi authenticationApi;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Test
    void shouldLoginWithAdminCredentials() throws Exception {
        // given - admin is created on application start

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
