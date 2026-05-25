package server.model.user;

public class Seller extends User {

    public Seller() {}

    public Seller(int id, String name,String tenHienThi, String password) {
        super(id, name,tenHienThi, password, "SELLER");
    }

    @Override
    public void displayRole() {
        System.out.println("I am Seller");
    }
}
