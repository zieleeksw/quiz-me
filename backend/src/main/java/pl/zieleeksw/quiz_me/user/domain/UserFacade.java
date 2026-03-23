package pl.zieleeksw.quiz_me.user.domain;

import org.springframework.security.crypto.password.PasswordEncoder;
import pl.zieleeksw.quiz_me.role.RoleDto;
import pl.zieleeksw.quiz_me.role.domain.RoleFacade;
import pl.zieleeksw.quiz_me.user.UserCredentialsDto;
import pl.zieleeksw.quiz_me.user.UserDto;

import java.util.NoSuchElementException;

public class UserFacade {

    private final UserRepository userRepository;

    private final EmailValidator emailValidator;

    private final PasswordValidator passwordValidator;

    private final RoleFacade roleFacade;

    private final PasswordEncoder passwordEncoder;

    public UserFacade(
            final UserRepository userRepository,
            final EmailValidator emailValidator,
            final PasswordValidator passwordValidator,
            final RoleFacade roleFacade,
            final PasswordEncoder passwordEncoder

    ) {
        this.userRepository = userRepository;
        this.emailValidator = emailValidator;
        this.passwordValidator = passwordValidator;
        this.roleFacade = roleFacade;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto registerUser(
            final String email,
            final String password
    ) {
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsByEmail(email)) {
            throw EmailAlreadyExistsException.forEmail(email);
        }

        final RoleDto roleUSER = roleFacade.findRoleUSEROrThrow();
        final String encodedPassword = passwordEncoder.encode(password);
        final User user = User.register(email, encodedPassword, roleUSER.id());

        final UserEntity entity = UserEntity.from(user);
        final UserEntity saved = userRepository.save(entity);

        return new UserDto(
                saved.getId(),
                saved.getEmail(),
                roleUSER.id()
        );
    }

    public UserDto findUserByEmailOrThrow(
            final String email
    ) {
        final UserEntity entity = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException(String.format("System error: Cannot find user for email:  %s", email))
                );

        return new UserDto(
                entity.getId(),
                entity.getEmail(),
                entity.getRoleId()
        );
    }

    public UserCredentialsDto findUserCredentialsByEmailOrThrow(
            final String email
    ) {
        final UserEntity entity = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException(String.format("System error: Cannot find user for email:  %s", email))
                );

        return new UserCredentialsDto(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getRoleId()
        );
    }

    public void validateEmail(
            final String email
    ) {
        emailValidator.validate(email);
    }

    public void validatePassword(
            final String password
    ) {
        passwordValidator.validate(password);
    }

    void initializeAdmin(
            final String adminEmail,
            final String adminPassword) {

        if (adminEmail == null || adminEmail.isBlank() ||
                adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        final RoleDto roleADMIN = roleFacade.findRoleByNameOrThrow("ADMIN");
        final String encodedPassword = passwordEncoder.encode(adminPassword);
        final User admin = User.register(adminEmail, encodedPassword, roleADMIN.id());

        final UserEntity entity = UserEntity.from(admin);
        userRepository.save(entity);
    }
}
