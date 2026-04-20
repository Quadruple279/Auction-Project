package server.model.item;
public class Art extends Item{
    private String artist;
    public Art(String id,String name,double basePrice,String description,String artist){
        super(id,name,basePrice,description);
        this.artist=artist;
    }
    @Override
    public void displayDetails(){
        System.out.println("Bức tranh "+name+" của họa sĩ "+artist);
    }
}
