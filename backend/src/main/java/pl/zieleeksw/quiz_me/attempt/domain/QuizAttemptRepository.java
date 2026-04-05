package pl.zieleeksw.quiz_me.attempt.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface QuizAttemptRepository extends Repository<QuizAttemptEntity, Long> {

    QuizAttemptEntity save(QuizAttemptEntity entity);

    List<QuizAttemptEntity> findAllByCourseIdAndUserIdOrderByFinishedAtDesc(Long courseId, Long userId);

    Optional<QuizAttemptEntity> findByIdAndCourseIdAndUserId(Long id, Long courseId, Long userId);
}
