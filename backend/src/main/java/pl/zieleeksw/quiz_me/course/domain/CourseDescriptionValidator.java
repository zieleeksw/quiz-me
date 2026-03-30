package pl.zieleeksw.quiz_me.course.domain;

class CourseDescriptionValidator {

    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 1000;

    void validate(
            final String description
    ) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Course description cannot be empty.");
        }

        final String normalized = description.trim();
        if (normalized.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Course description is too short. Min length is 10 characters.");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Course description is too long. Max length is 1000 characters.");
        }
    }
}
