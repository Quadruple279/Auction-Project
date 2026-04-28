package server.controller;

import server.model.user.*;
import server.exception.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationController {

    private Map<String, User> users = new HashMap<>();
    private User currentUser;

    public void register(String id, String name, String password, String role) {
        if (role.equalsIgnoreCase("BIDDER")) {
            users.put(id, new Bidder(id, name, password));
        } else if (role.equalsIgnoreCase("SELLER")) {
            users.put(id, new Seller(id, name, password));
        } else if (role.equalsIgnoreCase("ADMIN")) {
            users.put(id, new Admin(id, name, password));
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