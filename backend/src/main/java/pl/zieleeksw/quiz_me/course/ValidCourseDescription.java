package pl.zieleeksw.quiz_me.course;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestCourseDescriptionValidator.class)
@Documented
@interface ValidCourseDescription {

    String message() default "Course description is invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
