package pl.zieleeksw.quiz_me.course;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;

class RequestCourseNameValidator implements ConstraintValidator<ValidCourseName, String> {

    private final CourseFacade courseFacade;

    RequestCourseNameValidator(
            final CourseFacade courseFacade
    ) {
        this.courseFacade = courseFacade;
    }

    @Override
    public boolean isValid(
            final String name,
            final ConstraintValidatorContext context
    ) {
        try {
            courseFacade.validateName(name);
            return true;
        } catch (final IllegalArgumentException ex) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ex.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
