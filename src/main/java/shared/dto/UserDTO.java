package shared.dto;

public class UserDTO {
    private int id;
    private String name;
    private String displayName;
    private String role;
    public UserDTO(){}
    public UserDTO(int id, String name, String displayName, String role){
        this.id=id;
        this.name=name;
        this.displayName = displayName;
        this.role=role;
    }
    public void setDisplayName(String displayName){
        this.displayName= displayName;
    }
    public void setId(int id)       { this.id = id; }
    public void setName(String name){ this.name = name; }
    public void setRole(String role){ this.role = role; }

    public int getId(){return id;}
    public String getName(){
        return name;
    }
    public String getDisplayName(){return displayName;}
    public String getRole(){return role;}
}
