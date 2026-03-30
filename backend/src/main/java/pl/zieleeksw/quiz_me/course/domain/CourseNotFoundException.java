package pl.zieleeksw.quiz_me.course.domain;

public class CourseNotFoundException extends RuntimeException {

    private CourseNotFoundException(
            final String message
    ) {
        super(message);
    }

    public static CourseNotFoundException forId(
            final Long id
    ) {
        return new CourseNotFoundException(String.format("Course with id %s was not found.", id));
    }
}
