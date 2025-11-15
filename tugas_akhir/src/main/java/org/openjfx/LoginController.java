package org.openjfx;

import java.io.IOException;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField plainPasswordField;

    @FXML
    private CheckBox showPasswordCheckBox;

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> loginCollection;

    @FXML
    public void initialize() {
        // Inisialisasi koneksi database
        try {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("gym");
            loginCollection = database.getCollection("login");
            System.out.println("✅ MongoDB connected successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to MongoDB: " + e.getMessage());
            showAlert(AlertType.ERROR, "Database Error", "Gagal terhubung ke database: " + e.getMessage());
        }

        // Sinkronisasi visibilitas password field dan plain text field
        setupPasswordVisibility();
        
        // HAPUS pemanggilan setLoginWindowFullScreen() dari sini
        // Full screen akan diatur di LoginApp.java
    }

    private void setupPasswordVisibility() {
        plainPasswordField.managedProperty().bind(showPasswordCheckBox.selectedProperty());
        plainPasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());

        passwordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        passwordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());

        // Binding dua arah isi password
        plainPasswordField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = getPassword().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Form tidak lengkap", "Username dan Password wajib diisi.");
            return;
        }

        System.out.println("🔐 Attempting login for user: " + username);

        try {
            // Cari user berdasarkan username saja dulu
            Document user = loginCollection.find(Filters.eq("username", username)).first();

            if (user != null) {
                String dbPassword = user.getString("password");
                String role = user.getString("role");
                String nama = user.getString("nama");

                // Verifikasi password
                if (password.equals(dbPassword)) {
                    System.out.println("✅ Login successful: " + username + " (" + role + ")");
                    
                    if ("penjaga".equalsIgnoreCase(role)) {
                        handleSuccessfulLogin(username, nama, role, "/org/dashboard_penjaga/dashboard.fxml", "Dashboard Penjaga");
                    } else if ("admin".equalsIgnoreCase(role)) {
                        handleSuccessfulLogin(username, nama, role, "/org/dashboard_admin/dashboardAdmin.fxml", "Dashboard Admin");
                    } else {
                        showAlert(AlertType.WARNING, "Akses Ditolak", "Peran tidak dikenali: " + role);
                    }
                } else {
                    System.out.println("❌ Password mismatch for user: " + username);
                    showAlert(AlertType.ERROR, "Login Gagal", "Username atau password salah.");
                }
            } else {
                System.out.println("❌ User not found: " + username);
                showAlert(AlertType.ERROR, "Login Gagal", "Username atau password salah.");
            }
        } catch (Exception e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Terjadi kesalahan saat mengakses database: " + e.getMessage());
        }
    }

    private String getPassword() {
        return showPasswordCheckBox.isSelected() ? 
               plainPasswordField.getText() : 
               passwordField.getText();
    }

    // Method untuk handle login yang berhasil
    private void handleSuccessfulLogin(String username, String nama, String role, String fxmlPath, String title) {
        try {
            // Set username yang login
            if ("penjaga".equalsIgnoreCase(role)) {
                DashboardPenjaga.setLoggedInUsername(username);
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // Set controller data berdasarkan role
            if ("penjaga".equalsIgnoreCase(role)) {
                Object controller = loader.getController();
                if (controller instanceof DashboardController) {
                    DashboardController dashboardController = (DashboardController) controller;
                    dashboardController.setLoggedInUsername(username);
                    dashboardController.setUserData(nama, role);
                }
            }
            
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle(title + " - " + nama);
            
            // Set dashboard ke full screen
            currentStage.setMaximized(true);
            
            System.out.println("🚀 Dashboard loaded successfully for: " + username);
            
        } catch (IOException e) {
            System.err.println("❌ Failed to load dashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Gagal membuka dashboard: " + e.getMessage());
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Method untuk menutup koneksi database (dipanggil saat aplikasi ditutup)
    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("✅ MongoDB connection closed");
        }
    }
}