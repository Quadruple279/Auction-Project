package server.model.item;
public abstract class Item{
    protected String id;
    protected String name;
    protected double basePrice;
    protected String description;
    protected String seller_name;
    public Item(String id,String name,double basePrice,String description,String seller_name){
        this.id=id;
        this.name=name;
        this.basePrice=basePrice;
        this.description = description;
        this.seller_name = seller_name;
    }
    //tao ra setter va getter de cac lop subclass co the su dung ma ko can ghi de
    public String getId(){return id;}
    public String getName(){return name;}
    public double getBasePrice(){return basePrice;}
    public String getDescription() {return description;}
    public String getSeller_name(){return seller_name;}
    //tao ra 1 abs method de cac lop con buoc phai ghi de
    public abstract void displayDetails();
    public abstract String getType();
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setPrice(double price) {
        this.basePrice = price;
    }
}
