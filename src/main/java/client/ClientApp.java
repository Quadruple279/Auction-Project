package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.dao.DataStorage;


public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader( getClass().getResource("/fxml/LoginViewMoi.fxml") );
        Scene scene = new Scene(loader.load());
        stage.setTitle("BidNow - Đấu giá trực tuyến");
        stage.setX(50);
        stage.setY(0);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        server.dao.DataStorage.loadData();
        AuctionDataTest.setupAuctions();
        launch(args);
    }
}