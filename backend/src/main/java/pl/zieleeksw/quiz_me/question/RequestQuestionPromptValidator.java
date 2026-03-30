package pl.zieleeksw.quiz_me.question;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;

class RequestQuestionPromptValidator implements ConstraintValidator<ValidQuestionPrompt, String> {

    private final QuestionFacade questionFacade;

    RequestQuestionPromptValidator(
            final QuestionFacade questionFacade
    ) {
        this.questionFacade = questionFacade;
    }

    @Override
    public boolean isValid(
            final String prompt,
            final ConstraintValidatorContext context
    ) {
        try {
            questionFacade.validatePrompt(prompt);
            return true;
        } catch (final IllegalArgumentException ex) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ex.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
