package server.model.item;

import javax.xml.namespace.QName;

public class Vehicle extends Item {
    private int carYear;
    private String bienSoXe;
    public Vehicle(String id,String name,double basePrice,String description,int carYear,String bienSoXe){
        super(id,name,basePrice,description);
        this.carYear=carYear;
        this.bienSoXe=bienSoXe;
    }
    @Override
    public void displayDetails(){
        System.out.println("Chiếc xe "+name+" biến số: -"+bienSoXe+"- đời xe "+carYear);
    }
}
