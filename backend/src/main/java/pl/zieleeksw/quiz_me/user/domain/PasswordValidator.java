package pl.zieleeksw.quiz_me.user.domain;


import io.micrometer.common.util.StringUtils;

class PasswordValidator {

    private static final int MIN_LENGTH = 12;

    private static final int MAX_LENGTH = 128;

    void validate(final String password) {

        if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        if (password.length() > MAX_LENGTH) {
            final String error = String.format("Password is too long. Max length is %s characters.", MAX_LENGTH);
            throw new IllegalArgumentException(error);
        }

        if (password.length() < MIN_LENGTH) {
            final String error = String.format("Password is too short. Min length is %s characters.", MIN_LENGTH);
            throw new IllegalArgumentException(error);
        }
    }
}