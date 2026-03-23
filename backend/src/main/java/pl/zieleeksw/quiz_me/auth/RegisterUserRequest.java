package pl.zieleeksw.quiz_me.auth;


public record RegisterUserRequest(
        @ValidEmail String email,
        @ValidPassword String password
) {
}