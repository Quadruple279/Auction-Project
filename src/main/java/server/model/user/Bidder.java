package server.model.user;

public class Bidder extends User {

    public Bidder() {}

    public Bidder(int id, String name,String tenHienThi, String password) {
        super(id, name,tenHienThi, password, "BIDDER");
    }

    @Override
    public void displayRole() {
        System.out.println("I am Bidder");
    }
}
