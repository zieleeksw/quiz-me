package pl.zieleeksw.quiz_me.category.domain;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface CategoryRepository extends Repository<CategoryEntity, Long> {

    CategoryEntity save(CategoryEntity entity);

    Optional<CategoryEntity> findById(Long id);

    List<CategoryEntity> findAllByCourseIdAndActiveTrueOrderByNameAsc(Long courseId);

    List<CategoryEntity> findAllByIdInAndCourseIdAndActiveTrue(List<Long> ids, Long courseId);

    List<CategoryEntity> findAllByIdInAndCourseId(List<Long> ids, Long courseId);

    boolean existsByCourseIdAndActiveTrueAndNameIgnoreCase(Long courseId, String name);
}
