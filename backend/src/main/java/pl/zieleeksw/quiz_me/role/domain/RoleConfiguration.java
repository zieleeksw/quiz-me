package pl.zieleeksw.quiz_me.role.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RoleConfiguration {

    @Bean
    RoleFacade roleFacade(
            final RoleRepository roleRepository
    ) {
        return new RoleFacade(
                roleRepository
        );
    }

    @Bean
    RoleWarmup roleWarmup(
            final RoleFacade roleFacade
    ) {
        return new RoleWarmup(roleFacade);
    }
}
