package client;

import server.controller.AuthenticationController;

public class AppContext {
    private static AuthenticationController authController;
    private static String loggedInUsername;

    public static AuthenticationController getAuthController() {
        if (authController == null) {
            authController = new AuthenticationController(); // chỉ query DB 1 lần duy nhất
        }
        return authController;
    }

    public static String getLoggedInUsername() {
        return loggedInUsername;
    }

    public static void setLoggedInUsername(String username) {
        loggedInUsername = username;
    }
}