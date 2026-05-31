package client;

import java.util.HashMap;
import java.util.Map;

public class AppContext {

    private static String loggedInUsername;
    // Key: auctionId, Value: true nếu autobid đang bật
    private static final Map<String, Boolean> autoBidStates = new HashMap<>();

    public static String getLoggedInUsername() {
        return loggedInUsername;
    }

    public static void setLoggedInUsername(String username) {
        loggedInUsername = username;
    }

    public static void setAutoBidActive(String auctionId, boolean active) {
        autoBidStates.put(auctionId, active);
    }

    public static boolean isAutoBidActive(String auctionId) {
        return autoBidStates.getOrDefault(auctionId, false);

    }
}
