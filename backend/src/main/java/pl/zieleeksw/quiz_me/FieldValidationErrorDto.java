package pl.zieleeksw.quiz_me;

import java.time.Instant;
import java.util.List;

class FieldValidationErrorDto {

    private final Instant timestamp = Instant.now();
    private final String exception;
    private final List<FieldErrorDto> errors;

    FieldValidationErrorDto(final String exception, final List<FieldErrorDto> errors) {
        this.exception = exception;
        this.errors = errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getException() {
        return exception;
    }

    public List<FieldErrorDto> getErrors() {
        return errors;
    }

    record FieldErrorDto(
            String field,
            String message
    ) {
    }
}
