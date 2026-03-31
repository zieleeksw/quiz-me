package pl.zieleeksw.quiz_me.question;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;

import java.util.List;

class RequestQuestionCategoryIdsValidator implements ConstraintValidator<ValidQuestionCategoryIds, List<Long>> {

    private final QuestionFacade questionFacade;

    RequestQuestionCategoryIdsValidator(
            final QuestionFacade questionFacade
    ) {
        this.questionFacade = questionFacade;
    }

    @Override
    public boolean isValid(
            final List<Long> categoryIds,
            final ConstraintValidatorContext context
    ) {
        try {
            questionFacade.validateCategoryIds(categoryIds);
            return true;
        } catch (final IllegalArgumentException ex) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ex.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
