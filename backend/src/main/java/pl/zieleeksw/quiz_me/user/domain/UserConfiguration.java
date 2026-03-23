package pl.zieleeksw.quiz_me.user.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.zieleeksw.quiz_me.role.domain.RoleFacade;

@Configuration
class UserConfiguration {

    @Bean
    UserFacade userFacade(
            final UserRepository userRepository,
            final RoleFacade roleFacade,
            final PasswordEncoder passwordEncoder
    ) {
        return new UserFacade(
                userRepository,
                new EmailValidator(),
                new PasswordValidator(),
                roleFacade,
                passwordEncoder
        );
    }

    @Bean
    UserWarmup userWarmup(
            final UserFacade userFacade,
            @Value("${app.admin.email}") final String adminEmail,
            @Value("${app.admin.password}") final String adminPassword
    ) {
        return new UserWarmup(
                userFacade,
                adminEmail,
                adminPassword
        );
    }
}
