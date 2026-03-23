package pl.zieleeksw.quiz_me.role.domain;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

@Order(1)
class RoleWarmup implements CommandLineRunner {

    private final RoleFacade roleFacade;

    public RoleWarmup(
            final RoleFacade roleFacade
    ) {
        this.roleFacade = roleFacade;
    }

    @Override
    public void run(final String... args) {
        roleFacade.initializeRoles();
    }
}
