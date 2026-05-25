package shared.dto;

public class UserDTO {
    private int id;
    private String name;
    private String tenHienThi;
    private String role;
    public UserDTO(int id,String name,String tenHienThi,String role){
        this.id=id;
        this.name=name;
        this.tenHienThi=tenHienThi;
        this.role=role;
    }
    public int getId(){return id;}
    public String getName(){
        return name;
    }
    public String getTenHienThi(){return tenHienThi;}
    public String getRole(){return role;}
}
