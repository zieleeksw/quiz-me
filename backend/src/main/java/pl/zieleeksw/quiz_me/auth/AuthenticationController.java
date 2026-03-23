package pl.zieleeksw.quiz_me.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.zieleeksw.quiz_me.auth.domain.AuthenticationFacade;
import pl.zieleeksw.quiz_me.user.UserDto;

@RestController
@RequestMapping("/auth")
class AuthenticationController {

    private final AuthenticationFacade authenticationFacade;

    public AuthenticationController(
            final AuthenticationFacade authenticationFacade
    ) {
        this.authenticationFacade = authenticationFacade;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(
            @RequestBody @Valid RegisterUserRequest dto) {
        return authenticationFacade.register(
                dto.email(),
                dto.password());
    }

    @PostMapping("/login")
    public AuthenticationDto login(
            @RequestBody AuthenticationRequest request) {
        return authenticationFacade.login(
                request.email(),
                request.password());
    }

    @PostMapping("/refresh-token")
    public AuthenticationDto refreshToken(
            @RequestBody RefreshTokenRequest requestBody) {
        return authenticationFacade.refreshToken(
                requestBody.token());
    }

}
