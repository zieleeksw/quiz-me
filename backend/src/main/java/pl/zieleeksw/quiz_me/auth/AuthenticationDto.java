package pl.zieleeksw.quiz_me.auth;

import pl.zieleeksw.quiz_me.user.UserDto;

public record AuthenticationDto(
        UserDto user,
        JwtDto accessToken,
        JwtDto refreshToken
) {
}
