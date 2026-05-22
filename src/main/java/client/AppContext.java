package client;

import server.controller.AuthenticationController;

public class AppContext {
    private static AuthenticationController authController;

    public static AuthenticationController getAuthController() {
        if (authController == null) {
            authController = new AuthenticationController(); // chỉ query DB 1 lần duy nhất
        }
        return authController;
    }
}
