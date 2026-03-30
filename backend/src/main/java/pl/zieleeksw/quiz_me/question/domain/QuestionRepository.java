package pl.zieleeksw.quiz_me.question.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface QuestionRepository extends Repository<QuestionEntity, Long> {

    QuestionEntity save(QuestionEntity entity);

    Optional<QuestionEntity> findById(Long id);

    List<QuestionEntity> findAllByCourseIdOrderByCreatedAtDesc(Long courseId);
}
