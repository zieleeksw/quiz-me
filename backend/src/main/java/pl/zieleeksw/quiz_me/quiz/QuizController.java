package pl.zieleeksw.quiz_me.quiz;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zieleeksw.quiz_me.quiz.domain.QuizFacade;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/quizzes")
class QuizController {

    private final QuizFacade quizFacade;
    private final UserFacade userFacade;

    QuizController(
            final QuizFacade quizFacade,
            final UserFacade userFacade
    ) {
        this.quizFacade = quizFacade;
        this.userFacade = userFacade;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    List<QuizDto> fetchAll(
            @PathVariable final Long courseId
    ) {
        return quizFacade.fetchQuizzes(courseId);
    }

    @GetMapping("/{quizId}/versions")
    @PreAuthorize("isAuthenticated()")
    List<QuizVersionDto> fetchVersions(
            @PathVariable final Long courseId,
            @PathVariable final Long quizId
    ) {
        return quizFacade.fetchQuizVersions(courseId, quizId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    QuizDto create(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @RequestBody @Valid final CreateQuizRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizFacade.createQuiz(
                courseId,
                request.title(),
                request.mode(),
                request.randomCount(),
                request.questionOrder(),
                request.answerOrder(),
                request.questionIds(),
                request.categoryIds(),
                currentUser.id(),
                isAdmin(authentication)
        );
    }

    @PutMapping("/{quizId}")
    @PreAuthorize("isAuthenticated()")
    QuizDto update(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long quizId,
            @RequestBody @Valid final UpdateQuizRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizFacade.updateQuiz(
                courseId,
                quizId,
                request.title(),
                request.mode(),
                request.randomCount(),
                request.questionOrder(),
                request.answerOrder(),
                request.questionIds(),
                request.categoryIds(),
                currentUser.id(),
                isAdmin(authentication)
        );
    }

    @DeleteMapping("/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    void delete(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long quizId
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        quizFacade.deleteQuiz(
                courseId,
                quizId,
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
