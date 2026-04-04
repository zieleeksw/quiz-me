package pl.zieleeksw.quiz_me.quiz.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface QuizRepository extends Repository<QuizEntity, Long> {

    QuizEntity save(QuizEntity entity);

    Optional<QuizEntity> findById(Long id);

    List<QuizEntity> findAllByCourseIdOrderByCreatedAtDesc(Long courseId);
}
