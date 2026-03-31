package pl.zieleeksw.quiz_me.question;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestQuestionCategoryIdsValidator.class)
@Documented
@interface ValidQuestionCategoryIds {

    String message() default "Question category ids are invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
