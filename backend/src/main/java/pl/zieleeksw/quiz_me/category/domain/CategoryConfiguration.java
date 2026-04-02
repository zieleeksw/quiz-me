package pl.zieleeksw.quiz_me.category.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.domain.CurrentQuestionCategoryGuard;

@Configuration
class CategoryConfiguration {

    @Bean
    CategoryFacade categoryFacade(
            final CategoryRepository categoryRepository,
            final CourseFacade courseFacade,
            final CurrentQuestionCategoryGuard currentQuestionCategoryGuard
    ) {
        return new CategoryFacade(
                categoryRepository,
                courseFacade,
                new CategoryNameValidator(),
                currentQuestionCategoryGuard
        );
    }
}
