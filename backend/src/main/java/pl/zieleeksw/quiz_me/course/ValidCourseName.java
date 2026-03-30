package pl.zieleeksw.quiz_me.course;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestCourseNameValidator.class)
@Documented
@interface ValidCourseName {

    String message() default "Course name is invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
