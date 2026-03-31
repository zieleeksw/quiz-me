package pl.zieleeksw.quiz_me.category;

public record CreateCategoryRequest(
        @ValidCategoryName String name
) {
}
