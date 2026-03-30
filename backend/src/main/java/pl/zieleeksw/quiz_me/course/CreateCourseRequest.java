package pl.zieleeksw.quiz_me.course;

public record CreateCourseRequest(
        @ValidCourseName String name,
        @ValidCourseDescription String description
) {
}
