package pl.zieleeksw.quiz_me.category.domain;

public class CategoryNotFoundException extends RuntimeException {

    private CategoryNotFoundException(
            final String message
    ) {
        super(message);
    }

    public static CategoryNotFoundException forId(
            final Long id
    ) {
        return new CategoryNotFoundException(String.format("Category with id %s was not found.", id));
    }
}
