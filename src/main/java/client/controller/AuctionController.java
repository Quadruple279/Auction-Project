package client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import server.model.Auction;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuctionController implements Initializable{
    @FXML
    private TableView<Auction> tableView;
    @FXML
    private TableColumn<Auction,String> auction;
    @FXML
    private TableColumn<Auction,String> itemName;
    @FXML
    private TableColumn<Auction,String> description;
    @FXML
    private TableColumn<Auction,Double> price;
    @FXML
    private TableColumn<Auction,Double> highestBid;
    @FXML
    private TableColumn<Auction,String> owner;
    @FXML
    private TextArea console;
    @FXML
    private MenuItem disconnect;
    @FXML
    private Button back;

    private ObservableList<Auction> danhSach = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUpCotBang();
        loadDuLieu();
    }

    public void setUpCotBang() {
        auction.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        itemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        description.setCellValueFactory(new PropertyValueFactory<>("description"));
        price.setCellValueFactory(new PropertyValueFactory<>("price"));
        highestBid.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        owner.setCellValueFactory(new PropertyValueFactory<>("leadingBidder"));

        tableView.setItems(danhSach);
    }

    public void loadDuLieu() {
        System.out.println("San sang. Chua co du lieu"); // Tam thoi de nhu nay
    }

    public void log(String msg) {
        if (console != null) {
            console.appendText(msg + "\n");
        }
    }

    @FXML
    public void disconnect(ActionEvent actionEvent) {
        log("Đã ngắt kết nối.");
        switchScene("/fxml/LoginView.fxml");
    }
    @FXML
    public void back(ActionEvent actionEvent) {
        switchScene("/fxml/LoginView.fxml");
    }
    private void switchScene(String fxmlPath) { // Được sử dụng để chuyển đổi màn hình
        try {
            FXMLLoader loader = new FXMLLoader( getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage)  tableView.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            log("Loi: Khong the tai man hinh");
        }
    }
}
