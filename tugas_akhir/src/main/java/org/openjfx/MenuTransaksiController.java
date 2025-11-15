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

public class MenuTransaksiController {

    @FXML private Button btnTransaksiGym;
    @FXML private Button btnTransaksiSnack;
    @FXML private Button btnTambahSnack;
    @FXML private Button btnDataTransaksi;  // NEW BUTTON
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
        System.out.println("📊 Setting user data in MenuTransaksiController: " + nama + " (" + role + ")");
        
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
        System.out.println("🛡️ MenuTransaksiController initialized");
        
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
        
        // Setup button actions jika belum di-set di FXML
        setupButtonActions();
    }

    private void setupButtonActions() {
        // Pastikan button actions sudah di-set
        if (btnTransaksiGym != null && btnTransaksiGym.getOnAction() == null) {
            btnTransaksiGym.setOnAction(e -> handleTransaksiGymClick());
        }
        if (btnTransaksiSnack != null && btnTransaksiSnack.getOnAction() == null) {
            btnTransaksiSnack.setOnAction(e -> handleTransaksiSnackClick());
        }
        if (btnTambahSnack != null && btnTambahSnack.getOnAction() == null) {
            btnTambahSnack.setOnAction(e -> handleTambahSnackClick());
        }
        if (btnDataTransaksi != null && btnDataTransaksi.getOnAction() == null) {
            btnDataTransaksi.setOnAction(e -> handleDataTransaksiClick());
        }
        if (btnLogout != null && btnLogout.getOnAction() == null) {
            btnLogout.setOnAction(e -> handleLogoutClick());
        }
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
    private void handleTransaksiGymClick() {
        playButtonClickAnimation(btnTransaksiGym);
        loadTransaksiGymPage();
    }

    @FXML
    private void handleTransaksiSnackClick() {
        playButtonClickAnimation(btnTransaksiSnack);
        loadTransaksiSnackPage();
    }

    @FXML
    private void handleTambahSnackClick() {
        playButtonClickAnimation(btnTambahSnack);
        loadTambahSnackPage();
    }

    @FXML
    private void handleDataTransaksiClick() {
        System.out.println("📊 Opening Data Transaksi page...");
        playButtonClickAnimation(btnDataTransaksi);
        loadDataTransaksiPage();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/dashboard_penjaga/dashboard.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnLogout.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Teman Fitness Gym - Login");
            
            // Set login page ke full screen juga
            currentStage.setMaximized(true);
            
            System.out.println("✅ Kembali ke halaman login");
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman login: " + e.getMessage());
            showAlert("Error", "Gagal kembali ke halaman login: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadTransaksiGymPage() {
        try {
            // Load file FXML transaksi gym
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/pembayaran/transaksi.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnTransaksiGym.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Transaksi Gym - Teman Fitness");
            
            // Set halaman transaksi ke full screen
            currentStage.setMaximized(true);
            
            System.out.println("✅ Beralih ke halaman Transaksi Gym");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman transaksi gym: " + e.getMessage());
            showAlert("Error", 
                "Gagal membuka halaman transaksi gym.\n" +
                "Pastikan file /org/pembayaran/transaksi.fxml ada.\n" +
                "Error: " + e.getMessage(), 
                Alert.AlertType.ERROR);
        }
    }

    private void loadTransaksiSnackPage() {
        try {
            // Load file FXML transaksi snack
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/transaksi_snack/snack.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnTransaksiSnack.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Transaksi Snack - Teman Fitness");
            
            // Set halaman transaksi snack ke full screen
            currentStage.setMaximized(true);
            
            System.out.println("✅ Beralih ke halaman Transaksi Snack");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman transaksi snack: " + e.getMessage());
            showAlert("Error", 
                "Gagal membuka halaman transaksi snack.\n" +
                "Pastikan file /org/transaksi_snack/snack.fxml ada.\n" +
                "Error: " + e.getMessage(), 
                Alert.AlertType.ERROR);
        }
    }

    private void loadTambahSnackPage() {
        try {
            // Load file FXML stock management
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/stok/stok.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnTambahSnack.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Kelola Stok Snack - Teman Fitness");
            
            // Set halaman stock management ke full screen
            currentStage.setMaximized(true);
            
            System.out.println("✅ Beralih ke halaman Kelola Stok Snack");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman kelola stok snack: " + e.getMessage());
            showAlert("Error", 
                "Gagal membuka halaman kelola stok snack.\n" +
                "Pastikan file /org/stok/stok.fxml ada.\n" +
                "Error: " + e.getMessage(), 
                Alert.AlertType.ERROR);
        }
    }

    private void loadDataTransaksiPage() {
        try {
            // Load file FXML data transaksi
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/data_transaksi/data.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnDataTransaksi.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Data Transaksi - Teman Fitness");
            
            // Set halaman data transaksi ke full screen
            currentStage.setMaximized(true);
            
            System.out.println("✅ Beralih ke halaman Data Transaksi");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Gagal memuat halaman data transaksi: " + e.getMessage());
            showAlert("Error", 
                "Gagal membuka halaman data transaksi.\n" +
                "Pastikan file /org/data_transaksi/data.fxml ada.\n" +
                "Error: " + e.getMessage(), 
                Alert.AlertType.ERROR);
        }
    }

    private void playButtonClickAnimation(Button button) {
        if (button != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public void cleanup() {
        stopDateTimeUpdater();
        MongoDBConnector.closeConnection();
    }
}