package pl.zieleeksw.quiz_me;

import java.time.Instant;
import java.util.List;

public class TestFieldValidationErrorDto {

    private final Instant timestamp = Instant.now();
    private final String exception;
    private final List<TestFieldErrorDto> errors;

    public TestFieldValidationErrorDto(final String exception, final List<TestFieldErrorDto> errors) {
        this.exception = exception;
        this.errors = errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getException() {
        return exception;
    }

    public List<TestFieldErrorDto> getErrors() {
        return errors;
    }

    public record TestFieldErrorDto(
            String field,
            String message
    ) {
    }
}

