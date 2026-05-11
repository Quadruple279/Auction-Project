package server.model.item;
public abstract class Item{
    protected String id;
    protected String name;
    protected double basePrice;
    protected String description;
    protected String sellerId;
    public Item(String id,String name,double basePrice,String description,String sellerId){
        this.id=id;
        this.name=name;
        this.basePrice=basePrice;
        this.description = description;
        this.sellerId = sellerId;
    }
    //tao ra setter va getter de cac lop subclass co the su dung ma ko can ghi de
    public String getId(){return id;}
    public String getName(){return name;}
    public double getBasePrice(){return basePrice;}
    public String getDescription() {return description;}
    public String getSellerId(){return sellerId;}
    //tao ra 1 abs method de cac lop con buoc phai ghi de
    public abstract void displayDetails();
    public abstract String getType();
}
