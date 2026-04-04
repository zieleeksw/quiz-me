package pl.zieleeksw.quiz_me.quiz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum QuizOrderMode {

    FIXED("fixed"),
    RANDOM("random");

    private final String value;

    QuizOrderMode(
            final String value
    ) {
        this.value = value;
    }

    @JsonCreator
    public static QuizOrderMode fromValue(
            final String value
    ) {
        return Arrays.stream(values())
                .filter(mode -> mode.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported quiz order mode: " + value));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
