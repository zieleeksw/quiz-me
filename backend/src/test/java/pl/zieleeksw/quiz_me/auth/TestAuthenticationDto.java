package pl.zieleeksw.quiz_me.auth;

import pl.zieleeksw.quiz_me.user.TestUserDto;

public record TestAuthenticationDto(
        TestUserDto user,
        TestJwtDto accessToken,
        TestJwtDto refreshToken
) {
}
