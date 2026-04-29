package server.controller;

import server.dao.DataStorage;
import server.model.user.*;
import server.exception.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationController {

    private Map<String, User> users = new HashMap<>();
    // constructor để load từ DataStorage
    public AuthenticationController() {
        for (User user : DataStorage.users) {
            users.put(user.getId(), user);
        }
    }
    private User currentUser;

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
            DataStorage.users.add(newUser);      // lưu vào List (để ghi file)
            DataStorage.saveData();              // 👈 LƯU FILE
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