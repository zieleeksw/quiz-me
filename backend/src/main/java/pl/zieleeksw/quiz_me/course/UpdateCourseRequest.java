package pl.zieleeksw.quiz_me.course;

public record UpdateCourseRequest(
        @ValidCourseName String name,
        @ValidCourseDescription String description
) {
}
