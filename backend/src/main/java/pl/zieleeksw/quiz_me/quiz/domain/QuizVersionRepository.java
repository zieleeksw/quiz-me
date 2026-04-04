package pl.zieleeksw.quiz_me.quiz.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface QuizVersionRepository extends Repository<QuizVersionEntity, Long> {

    QuizVersionEntity save(QuizVersionEntity entity);

    Optional<QuizVersionEntity> findByQuizIdAndVersionNumber(Long quizId, int versionNumber);

    List<QuizVersionEntity> findAllByQuizIdOrderByVersionNumberDesc(Long quizId);
}
