package pl.zieleeksw.quiz_me.category.domain;

class CategoryNameValidator {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 120;

    void validate(
            final String name
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        if (name.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Category name is too short. Min length is 2 characters.");
        }

        if (name.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Category name is too long. Max length is 120 characters.");
        }
    }
}
