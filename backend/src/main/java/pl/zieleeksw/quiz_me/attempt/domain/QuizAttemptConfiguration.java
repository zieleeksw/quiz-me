package pl.zieleeksw.quiz_me.attempt.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            final QuizSessionRepository quizSessionRepository,
            final CourseFacade courseFacade,
            final QuizFacade quizFacade,
            final QuestionFacade questionFacade,
            final ObjectMapper objectMapper
    ) {
        return new QuizAttemptFacade(
                quizAttemptRepository,
                quizSessionRepository,
                courseFacade,
                quizFacade,
                questionFacade,
                objectMapper
        );
    }

    @Bean
    QuizSessionFacade quizSessionFacade(
            final QuizSessionRepository quizSessionRepository,
            final CourseFacade courseFacade,
            final QuizFacade quizFacade,
            final QuestionFacade questionFacade,
            final ObjectMapper objectMapper
    ) {
        return new QuizSessionFacade(
                quizSessionRepository,
                courseFacade,
                quizFacade,
                questionFacade,
                objectMapper
        );
    }
}
