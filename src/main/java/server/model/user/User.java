package server.model.user;

public abstract class User {
    protected String id;
    protected String name;
    protected String password;
    protected String role;

    public User(String id, String name, String password, String role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public abstract void displayRole();

    public String getName() {return name;}

    public String getRole() {
        return role;
    }
     public boolean checkPassword(String password) {
        return this.password.equals(password);
     }
}
