package pl.zieleeksw.quiz_me.attempt;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zieleeksw.quiz_me.attempt.domain.QuizAttemptFacade;
import pl.zieleeksw.quiz_me.attempt.domain.QuizSessionFacade;
import pl.zieleeksw.quiz_me.user.UserDto;
import pl.zieleeksw.quiz_me.user.domain.UserFacade;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}")
class QuizAttemptController {

    private final QuizAttemptFacade quizAttemptFacade;
    private final QuizSessionFacade quizSessionFacade;
    private final UserFacade userFacade;

    QuizAttemptController(
            final QuizAttemptFacade quizAttemptFacade,
            final QuizSessionFacade quizSessionFacade,
            final UserFacade userFacade
    ) {
        this.quizAttemptFacade = quizAttemptFacade;
        this.quizSessionFacade = quizSessionFacade;
        this.userFacade = userFacade;
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    List<QuizSessionDto> fetchCourseSessions(
            final Authentication authentication,
            @PathVariable final Long courseId
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizSessionFacade.fetchCourseSessions(courseId, currentUser.id());
    }

    @PostMapping("/quizzes/{quizId}/session")
    @PreAuthorize("isAuthenticated()")
    QuizSessionDto createOrResumeQuizSession(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long quizId
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizSessionFacade.createOrResumeSession(courseId, quizId, currentUser.id());
    }

    @PutMapping("/quizzes/{quizId}/session")
    @PreAuthorize("isAuthenticated()")
    QuizSessionDto updateQuizSession(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long quizId,
            @RequestBody @Valid final UpdateQuizSessionRequest request
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizSessionFacade.updateSession(
                courseId,
                quizId,
                currentUser.id(),
                request.currentIndex(),
                request.answers()
        );
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

    @GetMapping("/attempts/reviews")
    @PreAuthorize("isAuthenticated()")
    List<QuizAttemptDetailDto> fetchCourseAttemptReviews(
            final Authentication authentication,
            @PathVariable final Long courseId
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizAttemptFacade.fetchCourseAttemptReviews(courseId, currentUser.id());
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("isAuthenticated()")
    QuizAttemptDetailDto fetchAttemptDetail(
            final Authentication authentication,
            @PathVariable final Long courseId,
            @PathVariable final Long attemptId
    ) {
        final UserDto currentUser = userFacade.findUserByEmailOrThrow(authentication.getName());

        return quizAttemptFacade.fetchAttemptDetail(courseId, attemptId, currentUser.id());
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
