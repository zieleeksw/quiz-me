package pl.zieleeksw.quiz_me.user.domain;

public class EmailAlreadyExistsException extends RuntimeException {

    private EmailAlreadyExistsException(final String message) {
        super(message);
    }

    public static EmailAlreadyExistsException forEmail(final String email) {
        final String error = String.format("User with email %s already exists.", email);
        return new EmailAlreadyExistsException(error);
    }
}
