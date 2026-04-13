package server.model.item;
public class Vehicle extends Item {
    private int carYear;
    private String bienSoXe;
    public Vehicle(String id,String name,double basePrice,int carYear,String bienSoXe){
        super(id,name,basePrice);
        this.carYear=carYear;
        this.bienSoXe=bienSoXe;
    }
    @Override
    public void displayDetails(){
        System.out.println("Chiếc xe "+name+" biến số: -"+bienSoXe+"- đời xe "+carYear);
    }
}
