package pl.zieleeksw.quiz_me.role.domain;


import org.springframework.data.repository.Repository;
import pl.zieleeksw.quiz_me.role.RoleDto;

import java.util.List;

public interface RoleQueryRepository extends Repository<RoleEntity, Long> {

    List<RoleDto> findAllProjectedBy();

}

