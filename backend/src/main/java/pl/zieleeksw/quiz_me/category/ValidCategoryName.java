package pl.zieleeksw.quiz_me.category;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestCategoryNameValidator.class)
@Documented
@interface ValidCategoryName {

    String message() default "Category name is invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
