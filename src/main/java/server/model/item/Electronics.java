package server.model.item;

public class Electronics extends Item {
    private int warrantyMonths;
    public Electronics(String id, String name, double basePrice, String description,String seller_name, int wM){
        super(id, name, basePrice, description,seller_name);
        this.warrantyMonths = wM;
    }
    @Override
    public void displayDetails(){
        System.out.println("Thiết bị "+name+" được bảo hành "+warrantyMonths+" tháng ");
    }
    @Override
    public String getType(){return "ELECTRONICS";}
    //Getter
    public int getWarrantyMonths(){return warrantyMonths;}
}
