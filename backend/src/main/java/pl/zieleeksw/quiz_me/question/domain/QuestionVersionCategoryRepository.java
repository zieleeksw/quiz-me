package pl.zieleeksw.quiz_me.question.domain;

import org.springframework.data.repository.Repository;

import java.util.List;

interface QuestionVersionCategoryRepository extends Repository<QuestionVersionCategoryEntity, Long> {

    <S extends QuestionVersionCategoryEntity> List<S> saveAll(Iterable<S> entities);

    List<QuestionVersionCategoryEntity> findAllByQuestionVersionIdOrderByDisplayOrderAsc(Long questionVersionId);
}
