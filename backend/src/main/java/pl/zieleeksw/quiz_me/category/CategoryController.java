package pl.zieleeksw.quiz_me.category;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zieleeksw.quiz_me.category.domain.CategoryFacade;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/categories")
class CategoryController {

    private final CategoryFacade categoryFacade;
    private final UserFacade userFacade;

    CategoryController(
            final CategoryFacade categoryFacade,
            final UserFacade userFacade
    ) {
        this.categoryFacade = categoryFacade;
        this.userFacade = userFacade;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    List<CategoryDto> fetchAll(
            @PathVariable final Long courseId
    ) {
        return categoryFacade.fetchActiveCategories(courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    CategoryDto create(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @RequestBody @Valid final CreateCategoryRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return categoryFacade.createCategory(
                courseId,
                request.name(),
                currentUser.id(),
                isAdmin(authentication)
        );
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    CategoryDto update(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long categoryId,
            @RequestBody @Valid final UpdateCategoryRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return categoryFacade.updateCategory(
                courseId,
                categoryId,
                request.name(),
                currentUser.id(),
                isAdmin(authentication)
        );
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    void delete(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long categoryId
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        categoryFacade.deleteCategory(
                courseId,
                categoryId,
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
