package pl.zieleeksw.quiz_me;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.zieleeksw.quiz_me.auth.domain.InvalidRefreshTokenException;
import pl.zieleeksw.quiz_me.category.domain.CategoryNotFoundException;
import pl.zieleeksw.quiz_me.course.domain.CourseNotFoundException;
import pl.zieleeksw.quiz_me.question.domain.QuestionNotFoundException;
import pl.zieleeksw.quiz_me.user.domain.EmailAlreadyExistsException;

import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException ex) {

        final List<FieldValidationErrorDto.FieldErrorDto> fieldErrorDtos = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldValidationErrorDto.FieldErrorDto(
                                err.getField(),
                                err.getDefaultMessage()
                        )
                )
                .toList();

        final FieldValidationErrorDto response = new FieldValidationErrorDto(
                ex.getClass().getSimpleName(),
                fieldErrorDtos);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<RuntimeExceptionDto> handleEmailAlreadyExistsException(
            final EmailAlreadyExistsException ex) {
        final RuntimeExceptionDto response = new RuntimeExceptionDto(
                ex.getClass().getSimpleName(),
                ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(CourseNotFoundException.class)
    ResponseEntity<RuntimeExceptionDto> handleCourseNotFoundException(
            final CourseNotFoundException ex) {
        final RuntimeExceptionDto response = new RuntimeExceptionDto(
                ex.getClass().getSimpleName(),
                ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ResponseEntity<RuntimeExceptionDto> handleCategoryNotFoundException(
            final CategoryNotFoundException ex) {
        final RuntimeExceptionDto response = new RuntimeExceptionDto(
                ex.getClass().getSimpleName(),
                ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    ResponseEntity<RuntimeExceptionDto> handleQuestionNotFoundException(
            final QuestionNotFoundException ex) {
        final RuntimeExceptionDto response = new RuntimeExceptionDto(
                ex.getClass().getSimpleName(),
                ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<RuntimeExceptionDto> handleRuntimeException(
            final RuntimeException ex) {

        final RuntimeExceptionDto response = new RuntimeExceptionDto(
                ex.getClass().getSimpleName(),
                ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    ResponseEntity<Void> handleInternalAuthenticationServiceException() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<Void> handleInvalidRefreshTokenException() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<Void> handleAuthorizationDeniedException() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Void> handleAccessDeniedException() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();
    }
}
