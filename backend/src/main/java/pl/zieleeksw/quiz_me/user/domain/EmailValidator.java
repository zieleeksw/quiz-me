package pl.zieleeksw.quiz_me.user.domain;


import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

class EmailValidator {

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$";

    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);

    private static final int MAX_LENGTH = 255;

    private static final int MIN_LENGTH = 11;

    void validate(final String email) {

        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("Email address cannot be empty.");
        }

        if (email.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Email is too long. Max length is " + MAX_LENGTH + " characters.");
        }

        if (email.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Email is too short. Min length is " + MIN_LENGTH + " characters.");
        }

        boolean isValid = PATTERN.matcher(email).matches();

        if (!isValid) {
            throw new IllegalArgumentException("Email is invalid: " + email);
        }
    }
}
