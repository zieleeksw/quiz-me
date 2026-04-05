package pl.zieleeksw.quiz_me.attempt.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;
import pl.zieleeksw.quiz_me.quiz.domain.QuizFacade;

@Configuration
class QuizAttemptConfiguration {

    @Bean
    QuizAttemptFacade quizAttemptFacade(
            final QuizAttemptRepository quizAttemptRepository,
            final CourseFacade courseFacade,
            final QuizFacade quizFacade,
            final QuestionFacade questionFacade
    ) {
        return new QuizAttemptFacade(
                quizAttemptRepository,
                courseFacade,
                quizFacade,
                questionFacade
        );
    }
}
