package pl.zieleeksw.quiz_me.auth;

public record TestLoginRequest(
        String email,
        String password
) {
}
