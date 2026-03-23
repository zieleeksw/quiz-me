package pl.zieleeksw.quiz_me.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

class RequestPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private final UserFacade userFacade;

    public RequestPasswordValidator(
            final UserFacade userFacade
    ) {
        this.userFacade = userFacade;
    }

    @Override
    public boolean isValid(final String password, final ConstraintValidatorContext context) {
        try {
            userFacade.validatePassword(password);
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
