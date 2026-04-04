package pl.zieleeksw.quiz_me.quiz.domain;

import org.springframework.data.repository.Repository;

import java.util.List;

interface QuizVersionCategoryRepository extends Repository<QuizVersionCategoryEntity, Long> {

    <S extends QuizVersionCategoryEntity> List<S> saveAll(Iterable<S> entities);

    List<QuizVersionCategoryEntity> findAllByQuizVersionIdOrderByDisplayOrderAsc(Long quizVersionId);
}
