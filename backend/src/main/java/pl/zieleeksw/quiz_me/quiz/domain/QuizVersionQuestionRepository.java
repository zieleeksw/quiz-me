package pl.zieleeksw.quiz_me.quiz.domain;

import org.springframework.data.repository.Repository;

import java.util.List;

interface QuizVersionQuestionRepository extends Repository<QuizVersionQuestionEntity, Long> {

    <S extends QuizVersionQuestionEntity> List<S> saveAll(Iterable<S> entities);

    List<QuizVersionQuestionEntity> findAllByQuizVersionIdOrderByDisplayOrderAsc(Long quizVersionId);
}
