package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardPenjaga extends Application {
    private static Stage primaryStage;
    private static String loggedInUsername;

    @Override
    public void start(Stage primaryStage) throws Exception {
        DashboardPenjaga.primaryStage = primaryStage;
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/dashboard_penjaga/dashboard.fxml"));
        Parent root = loader.load();
        
        DashboardController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);
        controller.setLoggedInUsername(loggedInUsername);

        Scene scene = new Scene(root); // Tetap responsive
        
        primaryStage.setTitle("Dashboard Penjaga - " + loggedInUsername);
        primaryStage.setScene(scene);
        
        // Set full screen untuk dashboard
        primaryStage.setMaximized(true); // Tetap maximize seperti sebelumnya
        
        primaryStage.show();
    }

    public static void setLoggedInUsername(String username) {
        loggedInUsername = username;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}