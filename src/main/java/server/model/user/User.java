package server.model.user;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "role" // Phân biệt dựa vào trường role
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Bidder.class, name = "BIDDER"),
        @JsonSubTypes.Type(value = Seller.class, name = "SELLER"),
        @JsonSubTypes.Type(value = Admin.class, name = "ADMIN")
})

public abstract class User implements Serializable {

    protected int id;
    protected String name;
    protected String displayName;
    protected String password;
    protected String role;

    // ✅ constructor mặc định (cho Jackson)
    public User() {
    }

    public User(int id, String name, String displayName, String password, String role) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.password = password;
        this.role = role;
    }

    public abstract void displayRole();

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPassword(String newPassword) {
        this.password = newPassword;
    }
}