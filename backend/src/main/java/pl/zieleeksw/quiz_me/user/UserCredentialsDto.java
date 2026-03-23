package pl.zieleeksw.quiz_me.user;

public record UserCredentialsDto(
        Long id,
        String email,
        String password,
        Long roleId
) {
}
