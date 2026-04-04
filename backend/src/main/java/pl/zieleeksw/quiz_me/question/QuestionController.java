package pl.zieleeksw.quiz_me.question;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.zieleeksw.quiz_me.question.domain.QuestionFacade;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/questions")
class QuestionController {

    private final QuestionFacade questionFacade;
    private final UserFacade userFacade;

    QuestionController(
            final QuestionFacade questionFacade,
            final UserFacade userFacade
    ) {
        this.questionFacade = questionFacade;
        this.userFacade = userFacade;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    List<QuestionDto> fetchAll(
            @PathVariable final Long courseId
    ) {
        return questionFacade.fetchQuestions(courseId);
    }

    @GetMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    QuestionPageDto fetchPreview(
            @PathVariable final Long courseId,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "5") final int size,
            @RequestParam(required = false) final String search,
            @RequestParam(required = false) final Long categoryId
    ) {
        return questionFacade.fetchQuestionPreview(courseId, page, size, search, categoryId);
    }

    @GetMapping("/{questionId}/versions")
    @PreAuthorize("isAuthenticated()")
    List<QuestionVersionDto> fetchVersions(
            @PathVariable final Long courseId,
            @PathVariable final Long questionId
    ) {
        return questionFacade.fetchQuestionVersions(courseId, questionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    QuestionDto create(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @RequestBody @Valid final CreateQuestionRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return questionFacade.createQuestion(
                courseId,
                request.prompt(),
                request.answers(),
                request.categoryIds(),
                currentUser.id(),
                isAdmin(authentication)
        );
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("isAuthenticated()")
    QuestionDto update(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long questionId,
            @RequestBody @Valid final UpdateQuestionRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return questionFacade.updateQuestion(
                courseId,
                questionId,
                request.prompt(),
                request.answers(),
                request.categoryIds(),
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
