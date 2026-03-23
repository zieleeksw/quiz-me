package pl.zieleeksw.quiz_me.user.domain;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface UserRepository extends Repository<UserEntity, Long> {

    UserEntity save(final UserEntity entity);

    boolean existsByEmail(final String email);

    Optional<UserEntity> findByEmail(final String email);
}
