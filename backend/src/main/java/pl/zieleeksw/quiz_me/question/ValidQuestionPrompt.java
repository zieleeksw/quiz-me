package pl.zieleeksw.quiz_me.question;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestQuestionPromptValidator.class)
@Documented
@interface ValidQuestionPrompt {

    String message() default "Question prompt is invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
