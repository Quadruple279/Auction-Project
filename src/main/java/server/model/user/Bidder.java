package server.model.user;

public class Bidder extends User {

    public Bidder() {}

    public Bidder(String id, String name, String password) {
        super(id, name, password, "BIDDER");
    }

    @Override
    public void displayRole() {
        System.out.println("I am Bidder");
    }
}
