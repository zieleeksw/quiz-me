package pl.zieleeksw.quiz_me.course;

import java.time.Instant;

public record CourseDto(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Long ownerUserId,
        int questionCount,
        int quizCount,
        int progressPercent
) {
}
