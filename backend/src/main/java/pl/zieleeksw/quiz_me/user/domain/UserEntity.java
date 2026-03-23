package pl.zieleeksw.quiz_me.user.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    protected UserEntity() {
    }

    static UserEntity from(
            final User user
    ) {
        final UserEntity entity = new UserEntity();

        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setRoleId(user.getRoleId());

        return entity;
    }

    Long getId() {
        return id;
    }

    void setId(final Long id) {
        this.id = id;
    }

    String getEmail() {
        return email;
    }

    void setEmail(final String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    void setPassword(final String password) {
        this.password = password;
    }

    Long getRoleId() {
        return roleId;
    }

    void setRoleId(final Long roleId) {
        this.roleId = roleId;
    }
}
