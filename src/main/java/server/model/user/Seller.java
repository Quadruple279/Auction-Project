package server.model.user;

public class Seller extends User {

    public Seller(String id, String name) {
        super(id, name);
    }

    @Override
    public void displayRole() {
        System.out.println("I am Seller");
    }
}
