package pl.zieleeksw.quiz_me.auth;

public record AuthenticationRequest(
        String email,
        String password
) {
}
