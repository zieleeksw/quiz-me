package pl.zieleeksw.quiz_me.role.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    protected RoleEntity() {
    }

    static RoleEntity from(final Role role) {
        final RoleEntity entity = new RoleEntity();

        entity.setId(null);
        entity.setName(role.name());

        return entity;
    }

    String getName() {
        return name;
    }

    void setName(final String name) {
        this.name = name;
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
    }
}
