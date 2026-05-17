package server.model.item;

import javax.xml.namespace.QName;

public class Vehicle extends Item {
    private int carYear;
    private String bienSoXe;
    public Vehicle(String id,String name,double basePrice,String description,String seller_name,int carYear,String bienSoXe){
        super(id,name,basePrice,description,seller_name);
        this.carYear=carYear;
        this.bienSoXe=bienSoXe;
    }
    @Override
    public void displayDetails(){
        System.out.println("Chiếc xe "+name+" biến số: -"+bienSoXe+"- đời xe "+carYear);
    }
    @Override
    public String getType(){return "VEHICLE";}
    //Getter
    public int getCarYear(){return carYear;}
    public String getBienSoXe(){return bienSoXe;}
}
