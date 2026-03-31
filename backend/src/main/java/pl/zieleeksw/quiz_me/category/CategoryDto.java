package pl.zieleeksw.quiz_me.category;

import java.time.Instant;

public record CategoryDto(
        Long id,
        Long courseId,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
