package pl.zieleeksw.quiz_me.course;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zieleeksw.quiz_me.course.domain.CourseFacade;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

import java.util.List;

@RestController
@RequestMapping("/courses")
class CourseController {

    private final CourseFacade courseFacade;
    private final UserFacade userFacade;

    CourseController(
            final CourseFacade courseFacade,
            final UserFacade userFacade
    ) {
        this.courseFacade = courseFacade;
        this.userFacade = userFacade;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    List<CourseDto> fetchAll() {
        return courseFacade.fetchCourses();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    CourseDto create(
            final Authentication authentication,
            @RequestBody @Valid final CreateCourseRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return courseFacade.createCourse(
                request.name(),
                request.description(),
                currentUser.id()
        );
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("isAuthenticated()")
    CourseDto update(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @RequestBody @Valid final UpdateCourseRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return courseFacade.updateCourse(
                courseId,
                request.name(),
                request.description(),
                currentUser.id(),
                isAdmin(authentication)
        );
    }

    private boolean isAdmin(
            final Authentication authentication
    ) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
