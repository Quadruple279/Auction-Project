package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.dao.AuctionDAO;
import server.model.Auction;
import server.model.AuctionManager;


public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginViewMoi.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("BidNow - Đấu giá trực tuyến");
        stage.setX(50);
        stage.setY(0);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        try {
            AuctionDAO auctionDAO = new AuctionDAO();
            AuctionManager manager = AuctionManager.getInstance();
            for (Auction auction : auctionDAO.findAll()) {
                manager.addAuction(auction);
            }
            System.out.println("[DB] Đã load " + manager.getAuctionList().size() + " phiên đấu giá.");
        } catch (Exception e) {
            System.out.println("[DB] Lỗi load auction: " + e.getMessage());
        }
        launch(args);
    }
}