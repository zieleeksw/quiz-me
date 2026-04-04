package pl.zieleeksw.quiz_me.quiz.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.zieleeksw.quiz_me.category.domain.CategoryFacade;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;

@Configuration
class QuizConfiguration {

    @Bean
    QuizFacade quizFacade(
            final QuizRepository quizRepository,
            final QuizVersionRepository quizVersionRepository,
            final QuizVersionQuestionRepository quizVersionQuestionRepository,
            final QuizVersionCategoryRepository quizVersionCategoryRepository,
            final CourseFacade courseFacade,
            final CategoryFacade categoryFacade,
            final QuestionFacade questionFacade
    ) {
        return new QuizFacade(
                quizRepository,
                quizVersionRepository,
                quizVersionQuestionRepository,
                quizVersionCategoryRepository,
                courseFacade,
                categoryFacade,
                questionFacade
        );
    }
}
