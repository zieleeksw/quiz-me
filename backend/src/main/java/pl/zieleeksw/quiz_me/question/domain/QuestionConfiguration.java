package pl.zieleeksw.quiz_me.question.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.zieleeksw.quiz_me.category.domain.CategoryFacade;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;

@Configuration
class QuestionConfiguration {

    @Bean
    CurrentQuestionCategoryGuard currentQuestionCategoryGuard(
            final QuestionRepository questionRepository,
            final QuestionVersionRepository questionVersionRepository,
            final QuestionVersionCategoryRepository questionVersionCategoryRepository
    ) {
        return new CurrentQuestionCategoryGuard(
                questionRepository,
                questionVersionRepository,
                questionVersionCategoryRepository
        );
    }

    @Bean
    QuestionFacade questionFacade(
            final QuestionRepository questionRepository,
            final QuestionVersionRepository questionVersionRepository,
            final QuestionAnswerRepository questionAnswerRepository,
            final QuestionVersionCategoryRepository questionVersionCategoryRepository,
            final CourseFacade courseFacade,
            final CategoryFacade categoryFacade
    ) {
        return new QuestionFacade(
                questionRepository,
                questionVersionRepository,
                questionAnswerRepository,
                questionVersionCategoryRepository,
                courseFacade,
                categoryFacade,
                new QuestionPromptValidator(),
                new QuestionAnswersValidator(),
                new QuestionCategoryIdsValidator()
        );
    }
}
