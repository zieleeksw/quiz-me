package pl.zieleeksw.quiz_me.question.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;

@Configuration
class QuestionConfiguration {

    @Bean
    QuestionFacade questionFacade(
            final QuestionRepository questionRepository,
            final QuestionVersionRepository questionVersionRepository,
            final QuestionAnswerRepository questionAnswerRepository,
            final CourseFacade courseFacade
    ) {
        return new QuestionFacade(
                questionRepository,
                questionVersionRepository,
                questionAnswerRepository,
                courseFacade,
                new QuestionPromptValidator(),
                new QuestionAnswersValidator()
        );
    }
}
