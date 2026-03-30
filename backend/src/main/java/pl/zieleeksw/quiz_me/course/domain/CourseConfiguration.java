package pl.zieleeksw.quiz_me.course.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CourseConfiguration {

    @Bean
    CourseFacade courseFacade(
            final CourseRepository courseRepository
    ) {
        return new CourseFacade(
                courseRepository,
                new CourseNameValidator(),
                new CourseDescriptionValidator()
        );
    }
}
