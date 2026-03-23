package pl.zieleeksw.quiz_me.auth;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

class RequestEmailValidator implements ConstraintValidator<ValidEmail, String> {

    private final UserFacade userFacade;

    public RequestEmailValidator(
            final UserFacade userFacade
    ) {
        this.userFacade = userFacade;
    }

    @Override
    public boolean isValid(final String email, final ConstraintValidatorContext context) {
        try {
            userFacade.validateEmail(email);
            return true;
        } catch (final IllegalArgumentException e) {
            context.disableDefaultConstraintViolation();

            final String exceptionMessage = e.getMessage();
            context.buildConstraintViolationWithTemplate(exceptionMessage)
                    .addConstraintViolation();

            return false;
        }
    }
}
