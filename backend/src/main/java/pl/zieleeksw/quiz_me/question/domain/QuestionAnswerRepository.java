package pl.zieleeksw.quiz_me.question.domain;

import org.springframework.data.repository.Repository;

import java.util.List;

interface QuestionAnswerRepository extends Repository<QuestionAnswerEntity, Long> {

    <S extends QuestionAnswerEntity> List<S> saveAll(Iterable<S> entities);

    List<QuestionAnswerEntity> findAllByQuestionVersionIdOrderByDisplayOrderAsc(Long questionVersionId);
}
