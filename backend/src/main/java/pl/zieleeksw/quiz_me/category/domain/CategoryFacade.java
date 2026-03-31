package pl.zieleeksw.quiz_me.category.domain;

import org.springframework.security.access.AccessDeniedException;
import pl.zieleeksw.quiz_me.category.CategoryDto;
import pl.zieleeksw.quiz_me.course.CourseDto;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class CategoryFacade {

    private final CategoryRepository categoryRepository;

    private final CourseFacade courseFacade;

    private final CategoryNameValidator categoryNameValidator;

    CategoryFacade(
            final CategoryRepository categoryRepository,
            final CourseFacade courseFacade,
            final CategoryNameValidator categoryNameValidator
    ) {
        this.categoryRepository = categoryRepository;
        this.courseFacade = courseFacade;
        this.categoryNameValidator = categoryNameValidator;
    }

    public CategoryDto createCategory(
            final Long courseId,
            final String name,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final String normalizedName = normalize(name);
        validateName(normalizedName);
        assertNameAvailable(courseId, normalizedName);

        final Instant now = roundToDatabasePrecision(Instant.now());
        final Category category = Category.create(courseId, normalizedName, now);
        final CategoryEntity saved = categoryRepository.save(CategoryEntity.from(category));
        return mapToDto(saved);
    }

    public List<CategoryDto> fetchActiveCategories(
            final Long courseId
    ) {
        courseFacade.findCourseByIdOrThrow(courseId);

        return categoryRepository.findAllByCourseIdAndActiveTrueOrderByNameAsc(courseId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public CategoryDto updateCategory(
            final Long courseId,
            final Long categoryId,
            final String name,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final CategoryEntity entity = findActiveCategoryInCourse(categoryId, courseId);
        final String normalizedName = normalize(name);
        validateName(normalizedName);

        if (!entity.getName().equalsIgnoreCase(normalizedName)) {
            assertNameAvailable(courseId, normalizedName);
        }

        final Category category = Category.restore(
                entity.getId(),
                entity.getCourseId(),
                entity.getName(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        category.rename(normalizedName, roundToDatabasePrecision(Instant.now()));

        final CategoryEntity saved = categoryRepository.save(CategoryEntity.from(category));
        return mapToDto(saved);
    }

    public void deleteCategory(
            final Long courseId,
            final Long categoryId,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final CourseDto course = courseFacade.findCourseByIdOrThrow(courseId);
        assertCanManageCourse(course, actorUserId, isAdmin);

        final CategoryEntity entity = findActiveCategoryInCourse(categoryId, courseId);
        final Category category = Category.restore(
                entity.getId(),
                entity.getCourseId(),
                entity.getName(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        category.archive(roundToDatabasePrecision(Instant.now()));
        categoryRepository.save(CategoryEntity.from(category));
    }

    public List<CategoryDto> findActiveCategoriesByIdsOrThrow(
            final Long courseId,
            final List<Long> categoryIds
    ) {
        final List<CategoryEntity> entities = categoryRepository.findAllByIdInAndCourseIdAndActiveTrue(categoryIds, courseId);

        if (entities.size() != categoryIds.size()) {
            throw new IllegalArgumentException("Some selected categories are invalid for this course.");
        }

        return sortByRequestedOrder(entities, categoryIds)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<CategoryDto> findCategoriesByIdsInCourse(
            final Long courseId,
            final List<Long> categoryIds
    ) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }

        final List<CategoryEntity> entities = categoryRepository.findAllByIdInAndCourseId(categoryIds, courseId);

        if (entities.size() != categoryIds.size()) {
            throw new IllegalStateException("Some stored categories could not be resolved.");
        }

        return sortByRequestedOrder(entities, categoryIds)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void validateName(
            final String name
    ) {
        categoryNameValidator.validate(name);
    }

    private CategoryEntity findCategoryInCourse(
            final Long categoryId,
            final Long courseId
    ) {
        final CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> CategoryNotFoundException.forId(categoryId));

        if (!category.getCourseId().equals(courseId)) {
            throw CategoryNotFoundException.forId(categoryId);
        }

        return category;
    }

    private CategoryEntity findActiveCategoryInCourse(
            final Long categoryId,
            final Long courseId
    ) {
        final CategoryEntity category = findCategoryInCourse(categoryId, courseId);

        if (!category.isActive()) {
            throw CategoryNotFoundException.forId(categoryId);
        }

        return category;
    }

    private void assertNameAvailable(
            final Long courseId,
            final String name
    ) {
        if (categoryRepository.existsByCourseIdAndActiveTrueAndNameIgnoreCase(courseId, name)) {
            throw new IllegalArgumentException("Category name already exists in this course.");
        }
    }

    private List<CategoryEntity> sortByRequestedOrder(
            final List<CategoryEntity> categories,
            final List<Long> requestedIds
    ) {
        return categories.stream()
                .sorted(Comparator.comparingInt(category -> requestedIds.indexOf(category.getId())))
                .toList();
    }

    private CategoryDto mapToDto(
            final CategoryEntity entity
    ) {
        return new CategoryDto(
                entity.getId(),
                entity.getCourseId(),
                entity.getName(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String normalize(
            final String value
    ) {
        return value == null ? null : value.trim();
    }

    private Instant roundToDatabasePrecision(
            final Instant instant
    ) {
        long epochSecond = instant.getEpochSecond();
        long roundedMicros = (instant.getNano() + 500L) / 1_000L;

        if (roundedMicros == 1_000_000L) {
            epochSecond++;
            roundedMicros = 0L;
        }

        return Instant.ofEpochSecond(epochSecond, roundedMicros * 1_000L);
    }

    private void assertCanManageCourse(
            final CourseDto course,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        if (isAdmin || course.ownerUserId().equals(actorUserId)) {
            return;
        }

        throw new AccessDeniedException("You cannot manage categories for this course.");
    }
}
