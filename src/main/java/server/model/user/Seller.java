package server.model.user;

public class Seller extends User {

    public Seller(String id, String name, String password) {
        super(id, name, password, "SELLER");
    }

    @Override
    public void displayRole() {
        System.out.println("I am Seller");
    }
}
