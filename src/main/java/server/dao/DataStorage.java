package server.dao;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import server.model.user.User;
import server.model.Auction;

public class DataStorage {

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.registerModule(new JavaTimeModule());
    }

    private static final String USER_FILE = "users.json";
    private static final String AUCTION_FILE = "auctions.json";

    // dữ liệu trong RAM
    public static List<User> users = new ArrayList<>();
    public static List<Auction> auctions = new ArrayList<>();

    // ===== LOAD =====
    public static void loadData() {
        try {
            users = mapper.readValue(
                    new File(USER_FILE),
                    mapper.getTypeFactory().constructCollectionType(List.class, User.class)
            );
        } catch (Exception e) {
            users = new ArrayList<>();
        }

        try {
            auctions = mapper.readValue(
                    new File(AUCTION_FILE),
                    mapper.getTypeFactory().constructCollectionType(List.class, Auction.class)
            );
        } catch (Exception e) {
            auctions = new ArrayList<>();
        }

        System.out.println("Data loaded!");
    }

    // ===== SAVE =====
    public static void saveData() {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(USER_FILE), users);

            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(AUCTION_FILE), auctions);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Data saved!");
    }
}
