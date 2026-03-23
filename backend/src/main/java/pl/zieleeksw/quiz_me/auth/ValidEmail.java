package pl.zieleeksw.quiz_me.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestEmailValidator.class)
@Documented
@interface ValidEmail {

    String message() default "Email is not valid. It must follow a standard format (e.g., user@domain.com)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}