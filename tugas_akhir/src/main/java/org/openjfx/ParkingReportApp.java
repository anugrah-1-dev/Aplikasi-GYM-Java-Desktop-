package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ParkingReportApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Ganti dengan path yang benar ke file FXML laporan parkir Anda
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/report/report.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        primaryStage.setTitle("Laporan Parkir");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}