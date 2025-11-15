package org.openjfx;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardController {

    @FXML private Button btnDaftarMember;
    @FXML private Button btnTransaksi;
    @FXML private Button btnHistoryPenjaga;
    @FXML private Button btnLogout;
    @FXML private Label lblNamaPenjaga;
    @FXML private Label lblUserRole;
    @FXML private Label lblDateTime;

    private Stage primaryStage;
    private String loggedInUsername;
    private Timer timer;
    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter timeFormatter;

    // MongoDB Connection Class (inner class)
    public static class MongoDBConnector {
        private static final String CONNECTION_STRING = "mongodb://localhost:27017";
        private static final String DATABASE_NAME = "gym";
        
        private static MongoClient mongoClient;
        private static MongoDatabase database;
        
        public static MongoDatabase getDatabase() {
            if (database == null) {
                try {
                    mongoClient = MongoClients.create(CONNECTION_STRING);
                    database = mongoClient.getDatabase(DATABASE_NAME);
                    System.out.println("✅ Connected to MongoDB successfully");
                } catch (Exception e) {
                    System.err.println("❌ Failed to connect to MongoDB: " + e.getMessage());
                    throw new RuntimeException("Database connection failed", e);
                }
            }
            return database;
        }
        
        public static void closeConnection() {
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("✅ MongoDB connection closed");
            }
        }
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Set full screen saat primary stage di-set
        if (primaryStage != null) {
            primaryStage.setMaximized(true);
        }
    }

    public void setLoggedInUsername(String username) {
        this.loggedInUsername = username;
        loadUserData();
    }

    public void setUserData(String nama, String role) {
        System.out.println("📊 Setting user data in DashboardController: " + nama + " (" + role + ")");
        
        Platform.runLater(() -> {
            if (lblNamaPenjaga != null) {
                lblNamaPenjaga.setText("Halo, " + nama);
            }
            if (lblUserRole != null) {
                lblUserRole.setText("Role: " + role);
            }
        });
    }

    @FXML
    private void initialize() {
        System.out.println("🛡️ DashboardController initialized");
        
        // Inisialisasi formatter untuk tanggal dan waktu
        dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        // Set default values jika data belum di-loaded
        if (lblNamaPenjaga != null && (lblNamaPenjaga.getText() == null || lblNamaPenjaga.getText().isEmpty())) {
            lblNamaPenjaga.setText("Halo, Penjaga");
        }
        if (lblUserRole != null && (lblUserRole.getText() == null || lblUserRole.getText().isEmpty())) {
            lblUserRole.setText("Role: Penjaga Gym");
        }
        
        // Mulai update jam dan tanggal
        startDateTimeUpdater();
    }

    private void startDateTimeUpdater() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updateDateTime());
            }
        }, 0, 1000);
    }

    private void updateDateTime() {
        if (lblDateTime != null) {
            LocalDateTime now = LocalDateTime.now();
            String date = now.format(dateFormatter);
            String time = now.format(timeFormatter);
            lblDateTime.setText(date + " | " + time);
        }
    }

    private void stopDateTimeUpdater() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void loadUserData() {
        try {
            MongoDatabase database = MongoDBConnector.getDatabase();
            MongoCollection<Document> collection = database.getCollection("login");
            
            Document userDoc = collection.find(Filters.eq("username", loggedInUsername)).first();
            
            if (userDoc != null) {
                String nama = userDoc.getString("nama");
                String role = userDoc.getString("role");
                
                Platform.runLater(() -> {
                    if (lblNamaPenjaga != null) {
                        lblNamaPenjaga.setText("Halo, " + nama);
                    }
                    if (lblUserRole != null) {
                        lblUserRole.setText("Role: " + role);
                    }
                });
                
                System.out.println("✅ Data user loaded: " + nama + " (" + role + ")");
            } else {
                Platform.runLater(() -> {
                    if (lblNamaPenjaga != null) {
                        lblNamaPenjaga.setText("Halo, " + loggedInUsername);
                    }
                    if (lblUserRole != null) {
                        lblUserRole.setText("Role: Penjaga Gym");
                    }
                });
                System.out.println("⚠️ User data not found, using default");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                if (lblNamaPenjaga != null) {
                    lblNamaPenjaga.setText("Halo, " + loggedInUsername);
                }
                if (lblUserRole != null) {
                    lblUserRole.setText("Role: Penjaga Gym");
                }
            });
            showAlert("Warning", "Gagal memuat data user, menggunakan data default", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDaftarMemberClick() {
        playButtonClickAnimation(btnDaftarMember);
        loadRegistrasiGymPage();
    }

    @FXML
    private void handleTransaksiClick() {
        playButtonClickAnimation(btnTransaksi);
        loadTransaksiPage();
    }

    @FXML
    private void handleHistoryPenjagaClick() {
        playButtonClickAnimation(btnHistoryPenjaga);
        loadHistoryPenjagaPage();
    }

    @FXML
    private void handleLogoutClick() {
        playButtonClickAnimation(btnLogout);
        System.out.println("➡ Melakukan logout...");
        
        stopDateTimeUpdater();
        MongoDBConnector.closeConnection();
        loadLoginPage();
    }

    private void loadLoginPage() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/login/login.fxml"));
            Stage currentStage = (Stage) btnLogout.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("BISA GYM - Login");
            
            // Set login page ke full screen juga
            currentStage.setMaximized(true);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman login: " + e.getMessage());
            showAlert("Error", "Gagal kembali ke halaman login", Alert.AlertType.ERROR);
        }
    }

    private void loadRegistrasiGymPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_member/menu_member.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnDaftarMember.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("From Member - BISA GYM");
            
            // Set halaman registrasi ke full screen juga
            currentStage.setMaximized(true);
            
            System.out.println("✅ Beralih ke halaman Registrasi Member");
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman registrasi: " + e.getMessage());
            showAlert("Error", "Gagal membuka halaman registrasi member", Alert.AlertType.ERROR);
        }
    }

    private void loadTransaksiPage() {
    try {
        // Path yang paling umum untuk JavaFX project
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_transaksi/menu_transaksi.fxml"));
        Parent root = loader.load();
        
        Stage currentStage = (Stage) btnTransaksi.getScene().getWindow();
        Scene scene = new Scene(root);
        currentStage.setScene(scene);
        currentStage.setTitle("From Transaksi - BISA GYM");
        
        // Pastikan full screen dengan multiple approaches
        currentStage.setMaximized(true);
        
        // Tambahkan event handler untuk memastikan tetap full screen
        currentStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                Platform.runLater(() -> currentStage.setMaximized(true));
            }
        });
        
        System.out.println("✅ Beralih ke halaman Transaksi");
        
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("❌ Gagal memuat halaman transaksi: " + e.getMessage());
        showAlert("Error", "Gagal membuka halaman transaksi. Pastikan file transaksi.fxml ada di folder yang benar.", Alert.AlertType.ERROR);
    }
}

    private void loadHistoryPenjagaPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/cctv/cctv.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnHistoryPenjaga.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("History Penjaga - BISA GYM");
            
            // Set halaman history ke full screen juga
            currentStage.setMaximized(true);
            
            System.out.println("✅ Beralih ke halaman History Penjaga");
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman history penjaga: " + e.getMessage());
            showAlert("Error", "Gagal membuka halaman history penjaga", Alert.AlertType.ERROR);
        }
    }

    private void playButtonClickAnimation(Button button) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.05);
        st.setToY(1.05);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }
    
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void cleanup() {
        stopDateTimeUpdater();
        MongoDBConnector.closeConnection();
    }

    public void setUserInfo(String loggedInUserName2, String userRole) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUserInfo'");
    }
}