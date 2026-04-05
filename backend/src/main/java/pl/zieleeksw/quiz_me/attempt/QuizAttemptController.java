package pl.zieleeksw.quiz_me.attempt;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zieleeksw.quiz_me.attempt.domain.QuizAttemptFacade;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}")
class QuizAttemptController {

    private final QuizAttemptFacade quizAttemptFacade;
    private final UserFacade userFacade;

    QuizAttemptController(
            final QuizAttemptFacade quizAttemptFacade,
            final UserFacade userFacade
    ) {
        this.quizAttemptFacade = quizAttemptFacade;
        this.userFacade = userFacade;
    }

    @GetMapping("/attempts")
    @PreAuthorize("isAuthenticated()")
    List<QuizAttemptDto> fetchCourseAttempts(
            final Authentication authentication,
            @PathVariable final Long courseId
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizAttemptFacade.fetchCourseAttempts(courseId, currentUser.id());
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    QuizAttemptDto createQuizAttempt(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long quizId,
            @RequestBody @Valid final SubmitQuizAttemptRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizAttemptFacade.createAttempt(
                courseId,
                quizId,
                currentUser.id(),
                request.answers()
        );
    }
}
