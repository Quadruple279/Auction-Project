package server.controller;

import server.dao.DataStorage;
import server.dao.UserDAO;
import server.model.user.*;
import server.exception.AuthenticationException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationController {
    private final UserDAO userDAO = new UserDAO();

    private Map<String, User> users = new HashMap<>();
    // constructor để load từ DataBase
    public AuthenticationController() {
        try{
            for (User user : userDAO.findAll()){
                users.put(user.getId(),user);
            }
        } catch (SQLException e){
            System.out.println("Lỗi load user từ DataBase: " + e.getMessage());
        }
    }
    private User currentUser;
    // lưu vào DataBase
    public void register(String id, String name, String password, String role) {
        User newUser = null;

        if (role.equalsIgnoreCase("BIDDER")) {
            newUser = new Bidder(id, name, password);
        } else if (role.equalsIgnoreCase("SELLER")) {
            newUser = new Seller(id, name, password);
        } else if (role.equalsIgnoreCase("ADMIN")) {
            newUser = new Admin(id, name, password);
        }

        if (newUser != null) {
            users.put(id, newUser);              // lưu vào Map (runtime)
            try {
                userDAO.save(newUser);
            } catch (SQLException e) {
                System.out.println("Lỗi lưu user: " + e.getMessage());
            }
        }
    }

    public User login(String id, String password) throws AuthenticationException {
        User user = users.get(id);

        if (user == null || !user.checkPassword(password)) {
            throw new AuthenticationException("Sai tài khoản hoặc mật khẩu");
        }

        currentUser = user;
        return user;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}