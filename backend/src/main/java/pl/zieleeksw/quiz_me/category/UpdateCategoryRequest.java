package pl.zieleeksw.quiz_me.category;

public record UpdateCategoryRequest(
        @ValidCategoryName String name
) {
}
