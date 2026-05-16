package server.controller;

import server.dao.UserDAO;
import server.model.user.*;
import server.exception.AuthenticationException;
import shared.dto.UserDTO;

import java.util.ArrayList;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthenticationController {
    private final UserDAO userDAO = new UserDAO();

    private Map<String, User> users = new HashMap<>();

    // constructor để load từ DataBase
    public AuthenticationController() {
        try {
            for (User user : userDAO.findAll()) {
                users.put(user.getName(), user);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi load user từ DataBase: " + e.getMessage());
        }
    }

    private User currentUser;

    public void register(String name, String password, String role) {
        User newUser = null;

        if (role.equalsIgnoreCase("BIDDER")) {
            newUser = new Bidder(0, name, password);
        } else if (role.equalsIgnoreCase("SELLER")) {
            newUser = new Seller(0, name, password);
        } else if (role.equalsIgnoreCase("ADMIN")) {
            newUser = new Admin(0, name, password);
        }

        if (newUser != null) {
            users.put(name, newUser);              // lưu vào Map (runtime)
            try {
                userDAO.save(newUser);
            } catch (SQLException e) {
                System.out.println("Lỗi lưu user vào DB : " + e.getMessage());
            }
        }
    }

    public User login(String name, String password) throws AuthenticationException {
        User user = users.get(name);

        if (user == null || !user.checkPassword(password)) {
            throw new AuthenticationException("Sai tài khoản hoặc mật khẩu");
        }

        currentUser = user;
        return user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public UserDTO getCurrentUserDTO(){
        if (currentUser == null){
            return null;
        }
        return new UserDTO(currentUser.getId(),currentUser.getName(),currentUser.getRole());
    }
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public void removeUser(User user) {
        if (user != null) {
            users.remove(user.getName());
            try {
                userDAO.delete(user.getId());
            } catch (SQLException e) {
                System.out.println("Lỗi xóa user ra khỏi DB: " + e.getMessage());
            }
        }
    }
    public void logout() {
        this.currentUser = null;
    }
}