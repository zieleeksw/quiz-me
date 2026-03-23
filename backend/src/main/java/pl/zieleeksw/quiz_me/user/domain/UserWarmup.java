package pl.zieleeksw.quiz_me.user.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

class UserWarmup implements CommandLineRunner {

    private final UserFacade userFacade;
    private final String adminEmail;
    private final String adminPassword;

    UserWarmup(
            final UserFacade userFacade,
            @Value("${app.admin.email}") final String adminEmail,
            @Value("${app.admin.password}") final String adminPassword
    ) {
        this.userFacade = userFacade;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(final String... args) {
        userFacade.initializeAdmin(adminEmail, adminPassword);
    }
}
