package pl.zieleeksw.quiz_me.user;

public record UserDto(
        Long id,
        String email,
        Long roleId
) {
}
