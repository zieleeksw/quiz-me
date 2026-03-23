package pl.zieleeksw.quiz_me.role;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.zieleeksw.quiz_me.role.domain.RoleQueryRepository;

import java.util.List;

@RestController
@RequestMapping("/roles")
class RoleController {

    private final RoleQueryRepository roleQueryRepository;

    RoleController(
            final RoleQueryRepository roleQueryRepository) {
        this.roleQueryRepository = roleQueryRepository;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    List<RoleDto> fetchAllRoles() {
        return roleQueryRepository.findAllProjectedBy();
    }
}
