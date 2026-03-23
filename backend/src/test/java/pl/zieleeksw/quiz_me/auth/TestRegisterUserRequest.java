package pl.zieleeksw.quiz_me.auth;

public record TestRegisterUserRequest(
        String email,
        String password
) {
}