package pl.zieleeksw.quiz_me.auth.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import pl.zieleeksw.quiz_me.role.domain.RoleFacade;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

@Configuration
class JwtConfiguration {

    @Bean
    JwtFacade jwtFacade(
            final JwtProperties jwtProperties
    ) {
        return new JwtFacade(jwtProperties);
    }

    @Bean
    UserDetailsService userDetailsService(
            final UserFacade userFacade,
            final RoleFacade roleFacade
    ) {
        return new CustomUserDetailsService(
                userFacade,
                roleFacade
        );
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            final JwtFacade jwtFacade,
            final UserDetailsService userDetailsService
    ) {
        return new JwtAuthenticationFilter(
                jwtFacade,
                userDetailsService
        );
    }

}
