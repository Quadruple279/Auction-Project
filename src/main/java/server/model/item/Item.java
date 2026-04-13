package server.model.item;
public abstract class Item{
    protected String id;
    protected String name;
    protected double basePrice;
    public Item(String id,String name,double basePrice){
        this.id=id;
        this.name=name;
        this.basePrice=basePrice;
    }
    //tao ra setter va getter de cac lop subclass co the su dung ma ko can ghi de
    public String getName(){return name;}
    public double getBasePrice(){return basePrice;}
    //tao ra 1 abs method de cac lop con buoc phai ghi de
    public abstract void displayDetails();
}
