package server.model.item;

public class Electronics extends Item {
    private int warrantyMonths;
    public Electronics(String id,String name,double basePrice,int wM){
        super(id,name,basePrice);
        this.warrantyMonths=wM;
    }
    @Override
    public void displayDetails(){
        System.out.println("Thiết bị "+name+" được bảo hành "+warrantyMonths+" tháng ");
    }
}
