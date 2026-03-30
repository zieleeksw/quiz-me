package pl.zieleeksw.quiz_me.course;

import java.time.Instant;

public record TestCourseDto(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Long ownerUserId,
        int questionCount,
        int quizCount,
        int progressPercent
) {
    public int expectedQuestionCount() {
        return switch (Math.floorMod(id.intValue(), 3)) {
            case 0 -> 96;
            case 1 -> 214;
            default -> 138;
        };
    }

    public int expectedQuizCount() {
        return switch (Math.floorMod(id.intValue(), 3)) {
            case 0 -> 3;
            case 1 -> 6;
            default -> 4;
        };
    }

    public int expectedProgressPercent() {
        return switch (Math.floorMod(id.intValue(), 3)) {
            case 0 -> 12;
            case 1 -> 64;
            default -> 28;
        };
    }
}
