package server.model.item;
public abstract class Item{
    protected String id;
    protected String name;
    protected double basePrice;
    protected String description;
    public Item(String id,String name,double basePrice,String description){
        this.id=id;
        this.name=name;
        this.basePrice=basePrice;
        this.description = description;
    }
    //tao ra setter va getter de cac lop subclass co the su dung ma ko can ghi de
    public String getName(){return name;}
    public double getBasePrice(){return basePrice;}
    public String getDescription() {return description;}
    //tao ra 1 abs method de cac lop con buoc phai ghi de
    public abstract void displayDetails();
}
