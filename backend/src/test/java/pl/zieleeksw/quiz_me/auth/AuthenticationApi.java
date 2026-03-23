package pl.zieleeksw.quiz_me.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.zieleeksw.quiz_me.user.TestUserDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Component
public class AuthenticationApi {

    private final MockMvc mockMvc;

    private final ObjectMapper objectMapper;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AuthenticationApi(
            final MockMvc mockMvc,
            final ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public TestUserDto register(
            final String email,
            final String password) throws Exception {
        final var request = new TestRegisterUserRequest(email, password);

        final MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        final String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json, TestUserDto.class);
    }

    public TestAuthenticationDto login(
            final String email,
            final String password) throws Exception {
        final var request = new TestLoginRequest(email, password);

        final MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        final String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json, TestAuthenticationDto.class);
    }

    public TestAuthenticationDto registerAndLogin() throws Exception {
        final String email = RandomEmailGenerator.generate();
        final String password = RandomPasswordGenerator.generate();
        register(email, password);
        return login(email, password);
    }

    public TestAuthenticationDto loginAsDefaultAdmin() throws Exception {
        return login(adminEmail, adminPassword);
    }
}
