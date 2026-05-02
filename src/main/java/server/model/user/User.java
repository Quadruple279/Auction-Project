package server.model.user;

import java.io.Serializable;

public abstract class User implements Serializable {

    protected String id;
    protected String name;
    protected String password;
    protected String role;

    // ✅ constructor mặc định (cho Jackson)
    public User() {
    }

    public User(String id, String name, String password, String role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public abstract void displayRole();

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }
}