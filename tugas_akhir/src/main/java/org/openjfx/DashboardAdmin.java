package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DashboardAdmin extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize database connection when application starts
        DatabaseConnector.initialize();
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/dashboard_admin/dashboardAdmin.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setMaximized(true);
        stage.setTitle("Dashboard Admin");
        stage.setScene(scene);
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        stage.show();
    }

    @Override
    public void stop() {
        // Close database connection when application exits
        DatabaseConnector.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}