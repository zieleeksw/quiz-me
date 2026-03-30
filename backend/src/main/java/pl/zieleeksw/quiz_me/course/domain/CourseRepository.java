package pl.zieleeksw.quiz_me.course.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface CourseRepository extends Repository<CourseEntity, Long> {

    CourseEntity save(CourseEntity entity);

    Optional<CourseEntity> findById(Long id);

    List<CourseEntity> findAllByOrderByCreatedAtDesc();
}
