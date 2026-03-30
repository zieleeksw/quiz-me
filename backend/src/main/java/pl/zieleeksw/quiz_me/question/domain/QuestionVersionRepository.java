package pl.zieleeksw.quiz_me.question.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface QuestionVersionRepository extends Repository<QuestionVersionEntity, Long> {

    QuestionVersionEntity save(QuestionVersionEntity entity);

    Optional<QuestionVersionEntity> findByQuestionIdAndVersionNumber(Long questionId, int versionNumber);

    List<QuestionVersionEntity> findAllByQuestionIdOrderByVersionNumberDesc(Long questionId);
}
