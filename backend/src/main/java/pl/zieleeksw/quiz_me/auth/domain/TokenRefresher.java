package pl.zieleeksw.quiz_me.auth.domain;


import pl.zieleeksw.quiz_me.auth.AuthenticationDto;
import pl.zieleeksw.quiz_me.auth.JwtDto;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

class TokenRefresher {

    private final JwtFacade jwtFacade;

    private final UserFacade userFacade;

    TokenRefresher(
            final JwtFacade jwtFacade,
            final UserFacade userFacade
    ) {
        this.jwtFacade = jwtFacade;
        this.userFacade = userFacade;
    }

    public AuthenticationDto refresh(
            final String refreshToken
    ) {
        try {
            return performRefresh(refreshToken);
        } catch (final Exception e) {
            throw new InvalidRefreshTokenException(e);
        }
    }

    private AuthenticationDto performRefresh(
            final String refreshToken
    ) {
        final String email = jwtFacade.extractEmail(refreshToken);
        final UserDto user = userFacade.findUserByEmailOrThrow(email);

        if (!jwtFacade.isTokenValid(refreshToken, user.email())) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        final String newAccessToken = jwtFacade.generateAccessToken(user.email());
        return new AuthenticationDto(
                user,
                new JwtDto(newAccessToken),
                new JwtDto(refreshToken)
        );
    }
}