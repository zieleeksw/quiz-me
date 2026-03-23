package pl.zieleeksw.quiz_me.user.domain;

class User {

    private Long id;

    private String email;

    private String password;

    private Long roleId;

    static User register(final String email, final String password, final Long roleId) {
        final User user = new User();

        user.setEmail(email);
        user.setPassword(password);
        user.setRoleId(roleId);

        return user;
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

    String getPassword() {
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

