package pl.zieleeksw.quiz_me.role.domain;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface RoleRepository extends Repository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    Optional<RoleEntity> findById(final Long id);

    void save(final RoleEntity entity);

}
