package server.model.item;

import java.util.Map;

public class ItemFactory {
    public static Item creatItem(String type, String id, String name, double basePrice,String description, String info1,String info2){
        if (type == null || type.isEmpty()){
            return null;
        }
        String lowerType = type.toLowerCase();
        switch (lowerType){
            case "art":
                return new Art(id,name,basePrice,description,info1);
            case "electronics":
                try{
                    int wM = Integer.parseInt(info1);
                    return new Electronics(id,name,basePrice,description,wM);
                }
                catch (Exception e){
                    System.out.println("Loi: "+e.getMessage());
                }
            case "vehicle":
                try{
                    int carYear = Integer.parseInt(info1);
                    return new Vehicle(id,name,basePrice,description,carYear,info2);
                }
                catch (Exception e){
                    System.out.println("Loi: "+e.getMessage());
                }
            default:
                throw new IllegalArgumentException("Loại vật phẩm '" + type + "' không có được đấu giá.");
        }
    }
}
