package pl.zieleeksw.quiz_me.auth.domain;


import pl.zieleeksw.quiz_me.auth.AuthenticationDto;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

public class AuthenticationFacade {

    private final UserFacade userFacade;
    private final UserAuthenticator userAuthenticator;
    private final TokenRefresher tokenRefresher;

    AuthenticationFacade(
            final UserFacade userFacade,
            final UserAuthenticator userAuthenticator,
            final TokenRefresher tokenRefresher
    ) {
        this.userFacade = userFacade;
        this.userAuthenticator = userAuthenticator;
        this.tokenRefresher = tokenRefresher;
    }

    public UserDto register(
            final String email,
            final String password
    ) {
        return userFacade.registerUser(
                email,
                password
        );
    }

    public AuthenticationDto login(
            final String email,
            final String password
    ) {
        return userAuthenticator.authenticate(
                email,
                password
        );
    }

    public AuthenticationDto refreshToken(
            final String refreshToken
    ) {
        return tokenRefresher.refresh(
                refreshToken
        );
    }
}