package pl.zieleeksw.quiz_me.category;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.category.domain.CategoryFacade;

class RequestCategoryNameValidator implements ConstraintValidator<ValidCategoryName, String> {

    private final CategoryFacade categoryFacade;

    RequestCategoryNameValidator(
            final CategoryFacade categoryFacade
    ) {
        this.categoryFacade = categoryFacade;
    }

    @Override
    public boolean isValid(
            final String name,
            final ConstraintValidatorContext context
    ) {
        try {
            categoryFacade.validateName(name);
            return true;
        } catch (final IllegalArgumentException ex) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ex.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
