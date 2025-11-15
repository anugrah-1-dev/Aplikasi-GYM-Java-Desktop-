package org.openjfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class DashboardControllerAdmin {
    @FXML
    private Button reportButton;
    
    @FXML
    private Button revisiButton;
    
    @FXML
    private Button logoutButton;

    @FXML
    private Button buatAkun;

    @FXML 
    private void initialize() {
        reportButton.setOnAction(this::handleReportButton);
        revisiButton.setOnAction(this::handleRevisiButton);
        logoutButton.setOnAction(this::handleLogoutButton);
        buatAkun.setOnAction(this::handlebuatAkun);
    }

    @FXML
    private void handleReportButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/report/report.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) reportButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setTitle("Report");
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to load report page: " + e.getMessage());
            e.printStackTrace();
        }
    }

   @FXML
private void handleRevisiButton(ActionEvent event) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/revisi/revisi.fxml"));
        Parent root = loader.load();
        
        // Dapatkan stage saat ini
        Stage stage = (Stage) revisiButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Revisi Harga Parkir");
        stage.show();
    } catch (Exception e) {
        showAlert("Error", "Gagal membuka form revisi: " + e.getMessage());
        e.printStackTrace();
    }
}


@FXML
private void handlebuatAkun(ActionEvent event) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/buatakun/buatakun.fxml"));
        Parent root = loader.load();
        
        // Dapatkan stage saat ini
        Stage stage = (Stage) revisiButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Buat Atau Hapus Akun");
        stage.show();
    } catch (Exception e) {
        showAlert("Error", "Gagal membuka form revisi: " + e.getMessage());
        e.printStackTrace();
    }
}


    @FXML
    private void handleLogoutButton(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/login/login.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1311, 650));
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    void setLoggedInUsername(String username) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void setUserData(String nama, String role) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setUserInfo(String nama, String role) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUserInfo'");
    }
}