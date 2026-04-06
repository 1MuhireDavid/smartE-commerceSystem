package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.config.DatabaseConfig;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 720);
        primaryStage.setTitle("Smart E-Commerce System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseConfig.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
