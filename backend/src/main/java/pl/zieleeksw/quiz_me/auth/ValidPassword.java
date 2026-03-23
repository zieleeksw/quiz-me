package pl.zieleeksw.quiz_me.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestPasswordValidator.class)
@Documented
@interface ValidPassword {

    String message() default "Password is not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}