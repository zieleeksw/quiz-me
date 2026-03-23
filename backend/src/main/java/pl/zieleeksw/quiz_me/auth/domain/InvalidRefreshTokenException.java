package pl.zieleeksw.quiz_me.auth.domain;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(final Throwable cause) {
        super(cause);
    }

    public InvalidRefreshTokenException(final String message) {
        super(message);
    }
}
