package server.model.item;

public class Electronics extends Item {
    private int warrantyMonths;
    public Electronics(String id,String name,double basePrice,String description,int wM){
        super(id,name,basePrice,description);
        this.warrantyMonths=wM;
    }
    @Override
    public void displayDetails(){
        System.out.println("Thiết bị "+name+" được bảo hành "+warrantyMonths+" tháng ");
    }
}
