package pl.zieleeksw.quiz_me.role.domain;


import pl.zieleeksw.quiz_me.role.RoleDto;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

public class RoleFacade {

    private final RoleRepository roleRepository;

    public RoleFacade(
            final RoleRepository roleRepository
    ) {
        this.roleRepository = roleRepository;
    }

    public RoleDto findRoleUSEROrThrow() {
        final RoleEntity entity = roleRepository.findByName(Role.USER.name())
                .orElseThrow(() ->
                        new NoSuchElementException("System error: Cannot find USER role.")
                );

        return new RoleDto(
                entity.getId(),
                entity.getName()
        );
    }

    public RoleDto findRoleByIdOrThrow(final Long id) {
        final RoleEntity entity = roleRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(String.format("System error: Cannot find role by id:  %s", id))
                );

        return new RoleDto(
                entity.getId(),
                entity.getName()
        );
    }


    public RoleDto findRoleByNameOrThrow(final String name) {
        final RoleEntity entity = roleRepository.findByName(name)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("System error: Cannot find role by name: %s", name)));

        return new RoleDto(
                entity.getId(),
                entity.getName());
    }

    void initializeRoles() {
        Arrays.stream(Role.values())
                .forEach(roleEnum -> {
                            final Optional<RoleEntity> roleByNameOptional = roleRepository.findByName(roleEnum.name());
                            if (roleByNameOptional.isEmpty()) {
                                final RoleEntity newRole = RoleEntity.from(roleEnum);
                                roleRepository.save(newRole);
                            }
                        }
                );
    }
}
