package pl.zieleeksw.quiz_me.course;

import java.time.Instant;

public record TestCourseDto(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Long ownerUserId
) {
}
