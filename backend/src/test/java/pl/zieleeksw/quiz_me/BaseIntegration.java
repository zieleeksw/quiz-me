package pl.zieleeksw.quiz_me;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class BaseIntegration {

    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:latest";
    private static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE_NAME)
                .withDatabaseName("quizzes")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        postgres.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    private static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM quiz_sessions");
        jdbcTemplate.execute("DELETE FROM quiz_attempts");
        jdbcTemplate.execute("DELETE FROM quiz_version_categories");
        jdbcTemplate.execute("DELETE FROM quiz_version_questions");
        jdbcTemplate.execute("DELETE FROM quiz_versions");
        jdbcTemplate.execute("DELETE FROM quizzes");
        jdbcTemplate.execute("DELETE FROM question_version_categories");
        jdbcTemplate.execute("DELETE FROM question_answers");
        jdbcTemplate.execute("DELETE FROM question_versions");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.execute("DELETE FROM users WHERE email != 'admin@quiz.com'");
    }
}
