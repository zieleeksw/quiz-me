package pl.zieleeksw.quiz_me.auth.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

@Configuration
class AuthenticationConfiguration {

    @Bean
    AuthenticationFacade authenticationFacade(
            final UserFacade userFacade,
            final AuthenticationManager authenticationManager,
            final JwtFacade jwtFacade
    ) {
        return new AuthenticationFacade(
                userFacade,
                new UserAuthenticator(
                        authenticationManager,
                        jwtFacade,
                        userFacade
                ),
                new TokenRefresher(
                        jwtFacade,
                        userFacade
                )
        );
    }
}
