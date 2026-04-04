package pl.zieleeksw.quiz_me.quiz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum QuizMode {

    MANUAL("manual"),
    RANDOM("random");

    private final String value;

    QuizMode(
            final String value
    ) {
        this.value = value;
    }

    @JsonCreator
    public static QuizMode fromValue(
            final String value
    ) {
        return Arrays.stream(values())
                .filter(mode -> mode.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported quiz mode: " + value));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
