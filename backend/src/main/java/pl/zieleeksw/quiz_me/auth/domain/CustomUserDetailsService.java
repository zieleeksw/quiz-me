package pl.zieleeksw.quiz_me.auth.domain;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.zieleeksw.quiz_me.role.RoleDto;
import pl.zieleeksw.quiz_me.role.domain.RoleFacade;
import pl.zieleeksw.quiz_me.user.UserCredentialsDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

class CustomUserDetailsService implements UserDetailsService {

    private final UserFacade userFacade;
    private final RoleFacade roleFacade;

    public CustomUserDetailsService(
            final UserFacade userFacade,
            final RoleFacade roleFacade
    ) {
        this.userFacade = userFacade;
        this.roleFacade = roleFacade;
    }

    @Override
    public UserDetails loadUserByUsername(
            final String username
    ) throws UsernameNotFoundException {
        final UserCredentialsDto userCredentialsDto = userFacade.findUserCredentialsByEmailOrThrow(username);
        final RoleDto role = roleFacade.findRoleByIdOrThrow(userCredentialsDto.roleId());

        return new SecurityUser(
                userCredentialsDto.id(),
                userCredentialsDto.email(),
                userCredentialsDto.password(),
                role.name()
        );
    }

}
