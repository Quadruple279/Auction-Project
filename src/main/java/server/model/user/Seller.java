package server.model.user;

public class Seller extends User {

    public Seller() {}

    public Seller(int id, String name,String displayName, String password) {
        super(id, name,displayName, password, "SELLER");
    }

    @Override
    public void displayRole() {
        System.out.println("I am Seller");
    }
}
