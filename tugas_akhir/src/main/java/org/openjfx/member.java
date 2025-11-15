package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class member extends Application {
    
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        
        Parent root = FXMLLoader.load(getClass().getResource("/org/kartu/membercard.fxml"));
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.setTitle("Teman Fitness Gym - Login");
        
        // Set full screen untuk aplikasi pertama kali dibuka
        stage.setMaximized(true);
        
        stage.show();
    }

    @Override
    public void stop() {
        // Tutup koneksi database jika ada
        if (DatabaseConnector.class != null) {
            DatabaseConnector.close();
        }
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}