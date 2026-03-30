package pl.zieleeksw.quiz_me.course;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;

class RequestCourseDescriptionValidator implements ConstraintValidator<ValidCourseDescription, String> {

    private final CourseFacade courseFacade;

    RequestCourseDescriptionValidator(
            final CourseFacade courseFacade
    ) {
        this.courseFacade = courseFacade;
    }

    @Override
    public boolean isValid(
            final String description,
            final ConstraintValidatorContext context
    ) {
        try {
            courseFacade.validateDescription(description);
            return true;
        } catch (final IllegalArgumentException ex) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ex.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
