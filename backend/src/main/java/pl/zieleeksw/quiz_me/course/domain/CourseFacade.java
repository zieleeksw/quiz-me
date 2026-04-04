package pl.zieleeksw.quiz_me.course.domain;

import pl.zieleeksw.quiz_me.course.CourseDto;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;

public class CourseFacade {

    private final CourseRepository courseRepository;

    private final CourseNameValidator courseNameValidator;

    private final CourseDescriptionValidator courseDescriptionValidator;

    CourseFacade(
            final CourseRepository courseRepository,
            final CourseNameValidator courseNameValidator,
            final CourseDescriptionValidator courseDescriptionValidator
    ) {
        this.courseRepository = courseRepository;
        this.courseNameValidator = courseNameValidator;
        this.courseDescriptionValidator = courseDescriptionValidator;
    }

    public CourseDto createCourse(
            final String name,
            final String description,
            final Long ownerUserId
    ) {
        final String normalizedName = normalize(name);
        final String normalizedDescription = normalize(description);

        validateName(normalizedName);
        validateDescription(normalizedDescription);

        final Course course = Course.create(
                normalizedName,
                normalizedDescription,
                roundToDatabasePrecision(Instant.now()),
                ownerUserId
        );

        final CourseEntity saved = courseRepository.save(CourseEntity.from(course));
        return mapToDto(saved);
    }

    public List<CourseDto> fetchCourses() {
        return courseRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public CourseDto findCourseByIdOrThrow(
            final Long id
    ) {
        final CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> CourseNotFoundException.forId(id));

        return mapToDto(entity);
    }

    public CourseDto updateCourse(
            final Long id,
            final String name,
            final String description,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        final String normalizedName = normalize(name);
        final String normalizedDescription = normalize(description);

        validateName(normalizedName);
        validateDescription(normalizedDescription);

        final CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> CourseNotFoundException.forId(id));

        final Course course = Course.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getOwnerUserId()
        );

        assertCanManageCourse(course, actorUserId, isAdmin);

        course.update(normalizedName, normalizedDescription);

        final CourseEntity saved = courseRepository.save(CourseEntity.from(course));
        return mapToDto(saved);
    }

    public void validateName(
            final String name
    ) {
        courseNameValidator.validate(name);
    }

    public void validateDescription(
            final String description
    ) {
        courseDescriptionValidator.validate(description);
    }

    private CourseDto mapToDto(
            final CourseEntity entity
    ) {
        return new CourseDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getOwnerUserId(),
                placeholderQuestionCount(entity),
                placeholderQuizCount(entity),
                placeholderProgressPercent(entity)
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
            final Course course,
            final Long actorUserId,
            final boolean isAdmin
    ) {
        if (isAdmin || course.getOwnerUserId().equals(actorUserId)) {
            return;
        }

        throw new AccessDeniedException("You cannot manage this course.");
    }

    private int placeholderQuestionCount(
            final CourseEntity entity
    ) {
        // TODO: Replace placeholder catalog metrics with real aggregates from the course question bank.
        return switch (Math.floorMod(entity.getId().intValue(), 3)) {
            case 0 -> 96;
            case 1 -> 214;
            default -> 138;
        };
    }

    private int placeholderQuizCount(
            final CourseEntity entity
    ) {
        // TODO: Replace placeholder quiz totals with real aggregated counts from persisted course quizzes.
        return switch (Math.floorMod(entity.getId().intValue(), 3)) {
            case 0 -> 3;
            case 1 -> 6;
            default -> 4;
        };
    }

    private int placeholderProgressPercent(
            final CourseEntity entity
    ) {
        // TODO: Replace placeholder progress with real learner/course progress aggregation.
        return switch (Math.floorMod(entity.getId().intValue(), 3)) {
            case 0 -> 12;
            case 1 -> 64;
            default -> 28;
        };
    }
}
