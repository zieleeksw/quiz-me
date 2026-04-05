package pl.zieleeksw.quiz_me.attempt.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface QuizSessionRepository extends Repository<QuizSessionEntity, Long> {

    QuizSessionEntity save(QuizSessionEntity entity);

    Optional<QuizSessionEntity> findByCourseIdAndQuizIdAndUserId(Long courseId, Long quizId, Long userId);

    List<QuizSessionEntity> findAllByCourseIdAndUserIdOrderByUpdatedAtDesc(Long courseId, Long userId);

    void delete(QuizSessionEntity entity);
}
