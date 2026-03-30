package pl.zieleeksw.quiz_me.question;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;

import java.util.List;

class RequestQuestionAnswersValidator implements ConstraintValidator<ValidQuestionAnswers, List<QuestionAnswerRequest>> {

    private final QuestionFacade questionFacade;

    RequestQuestionAnswersValidator(
            final QuestionFacade questionFacade
    ) {
        this.questionFacade = questionFacade;
    }

    @Override
    public boolean isValid(
            final List<QuestionAnswerRequest> answers,
            final ConstraintValidatorContext context
    ) {
        try {
            questionFacade.validateAnswers(answers);
            return true;
        } catch (final IllegalArgumentException ex) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ex.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
}
