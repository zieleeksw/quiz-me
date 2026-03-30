package pl.zieleeksw.quiz_me.question;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestQuestionAnswersValidator.class)
@Documented
@interface ValidQuestionAnswers {

    String message() default "Question answers are invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
