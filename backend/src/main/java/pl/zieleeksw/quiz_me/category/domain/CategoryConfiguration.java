package pl.zieleeksw.quiz_me.category.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;

@Configuration
class CategoryConfiguration {

    @Bean
    CategoryFacade categoryFacade(
            final CategoryRepository categoryRepository,
            final CourseFacade courseFacade
    ) {
        return new CategoryFacade(
                categoryRepository,
                courseFacade,
                new CategoryNameValidator()
        );
    }
}
