package pl.zieleeksw.quiz_me.attempt.domain;

import org.springframework.data.repository.Repository;

import java.util.List;

interface QuizAttemptRepository extends Repository<QuizAttemptEntity, Long> {

    QuizAttemptEntity save(QuizAttemptEntity entity);

    List<QuizAttemptEntity> findAllByCourseIdAndUserIdOrderByFinishedAtDesc(Long courseId, Long userId);
}
