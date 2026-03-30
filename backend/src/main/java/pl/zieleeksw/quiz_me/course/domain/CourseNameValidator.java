package pl.zieleeksw.quiz_me.course.domain;

class CourseNameValidator {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 120;

    void validate(
            final String name
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be empty.");
        }

        final String normalized = name.trim();
        if (normalized.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Course name is too short. Min length is 3 characters.");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Course name is too long. Max length is 120 characters.");
        }
    }
}
