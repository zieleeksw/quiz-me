package pl.zieleeksw.quiz_me.auth.domain;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pl.zieleeksw.quiz_me.auth.AuthenticationDto;
import pl.zieleeksw.quiz_me.auth.JwtDto;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

class UserAuthenticator {

    private final AuthenticationManager authenticationManager;
    private final JwtFacade jwtFacade;
    private final UserFacade userFacade;

    UserAuthenticator(
            final AuthenticationManager authenticationManager,
            final JwtFacade jwtFacade,
            final UserFacade userFacade
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtFacade = jwtFacade;
        this.userFacade = userFacade;
    }

    public AuthenticationDto authenticate(final String email,
                                          final String password
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        final UserDto user = userFacade.findUserByEmailOrThrow(email);

        final String accessToken = jwtFacade.generateAccessToken(email);
        final String refreshToken = jwtFacade.generateRefreshToken(email);

        return new AuthenticationDto(
                user,
                new JwtDto(accessToken),
                new JwtDto(refreshToken)
        );
    }
}
