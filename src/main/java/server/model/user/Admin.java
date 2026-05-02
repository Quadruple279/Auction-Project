package server.model.user;

public class Admin extends User {

    public  Admin() {}

    public Admin(String id, String name, String password) {
        super(id, name, password, "ADMIN");
    }

    @Override
    public void displayRole() {
        System.out.println("I am Admin");
    }
}
