package org.openjfx;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MemberController implements Initializable {

    @FXML
    private Button btnDaftarMember;
    
    @FXML
    private Button btnPerbaruiKartu;
    
    @FXML
    private Button btnAbsenMember;
    
    @FXML
    private Button btnLogout;
    
    @FXML
    private Label lblDateTime;
    
    @FXML
    private Label lblNamaPenjaga;
    
    @FXML
    private Label lblUserRole;
    
    @FXML
    private Label lblStatus;
    
    @FXML
    private Pane mainPane;
    
    @FXML
    private Pane statusBar;

    private Timeline timeline;
    private String loggedInUserName;
    private String userRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set user info if available
        if (loggedInUserName != null) {
            lblNamaPenjaga.setText(loggedInUserName);
        }
        if (userRole != null) {
            lblUserRole.setText(userRole);
        }
        
        // Set status message dengan branding BISA GYM
        lblStatus.setText("Sistem Manajemen Member - BISA GYM | Ready");
        
        // Initialize and start real-time clock
        initializeClock();
        
        // Setup button actions
        setupButtonActions();
    }

    /**
     * Initialize real-time clock
     */
    private void initializeClock() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy | HH:mm:ss");
            
            // Set initial time
            lblDateTime.setText(dateFormat.format(new Date()));
            
            // Timeline untuk update waktu setiap detik
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                String currentDateTime = dateFormat.format(new Date());
                lblDateTime.setText(currentDateTime);
            }));
            
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
            
        } catch (Exception e) {
            System.err.println("Error initializing clock: " + e.getMessage());
            lblDateTime.setText("Error loading time");
            showAlert(AlertType.ERROR, "Error", "Gagal Memuat Waktu", 
                     "Terjadi kesalahan saat memuat waktu sistem: " + e.getMessage());
        }
    }

    /**
     * Setup button actions programmatically
     */
    private void setupButtonActions() {
        // Backup action handlers - hanya jika belum di-set di FXML
        if (btnDaftarMember != null && btnDaftarMember.getOnAction() == null) {
            btnDaftarMember.setOnAction(event -> handleDaftarMemberClick());
        }
        
        if (btnPerbaruiKartu != null && btnPerbaruiKartu.getOnAction() == null) {
            btnPerbaruiKartu.setOnAction(event -> handlePerbaruiKartuClick());
        }
        
        if (btnAbsenMember != null && btnAbsenMember.getOnAction() == null) {
            btnAbsenMember.setOnAction(event -> handleAbsenMemberClick());
        }
        
        if (btnLogout != null && btnLogout.getOnAction() == null) {
            btnLogout.setOnAction(event -> handleLogoutClick());
        }
    }

    /**
     * Handle Daftar Member button click - Membuka form Registrasi Gym
     */
    @FXML
    private void handleDaftarMemberClick() {
        try {
            // Update status
            lblStatus.setText("Membuka form pendaftaran member baru...");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/karcis/RegistrasiGym.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnDaftarMember.getScene().getWindow();
            Scene scene = new Scene(root);
            
            currentStage.setScene(scene);
            currentStage.setTitle("Registrasi Member - BISA GYM");
            currentStage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Error: Gagal membuka form Daftar Member");
            showAlert(AlertType.ERROR, "Error", "Gagal membuka form Daftar Member", 
                     "Tidak dapat memuat halaman RegistrasiGym.fxml\n\nError: " + e.getMessage());
        }
    }

    /**
     * Handle Perbarui Kartu Member button click - Membuka form Update Member
     */
    @FXML
    private void handlePerbaruiKartuClick() {
        try {
            // Update status
            lblStatus.setText("Membuka form pembaruan kartu member...");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/update_member/update_member.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnPerbaruiKartu.getScene().getWindow();
            Scene scene = new Scene(root);
            
            currentStage.setScene(scene);
            currentStage.setTitle("Update Data Member - BISA GYM");
            currentStage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Error: Gagal membuka form Update Member");
            showAlert(AlertType.ERROR, "Error", "Gagal membuka form Update Member", 
                     "Tidak dapat memuat halaman update_member.fxml\n\nError: " + e.getMessage());
        }
    }

    /**
     * Handle Absen Member button click
     */
    @FXML
    private void handleAbsenMemberClick() {
        try {
            // Update status
            lblStatus.setText("Membuka form absensi member...");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/absen/absen.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) btnAbsenMember.getScene().getWindow();
            Scene scene = new Scene(root);
            
            currentStage.setScene(scene);
            currentStage.setTitle("Absen Member - BISA GYM");
            currentStage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Error: Gagal membuka form Absen");
            showAlert(AlertType.ERROR, "Error", "Gagal membuka form Absen", 
                     "Tidak dapat memuat halaman absen.fxml\n\nError: " + e.getMessage());
        }
    }

    /**
     * Handle Logout button click - Kembali ke Dashboard
     */
    @FXML
    private void handleLogoutClick() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Logout");
        alert.setHeaderText("Apakah Anda yakin ingin logout?");
        alert.setContentText("Anda akan kembali ke halaman Dashboard.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                returnToDashboard();
            }
        });
    }

    /**
     * Return to Dashboard
     */
    private void returnToDashboard() {
        try {
            // Stop timeline
            if (timeline != null) {
                timeline.stop();
            }
            
            // Update status
            lblStatus.setText("Kembali ke Dashboard...");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/dashboard_penjaga/dashboard.fxml"));
            Parent root = loader.load();
            
            // Pass user info to dashboard controller jika diperlukan
            DashboardController dashboardController = loader.getController();
            if (dashboardController != null && loggedInUserName != null) {
                dashboardController.setUserInfo(loggedInUserName, userRole);
            }
            
            Stage currentStage = (Stage) btnLogout.getScene().getWindow();
            Scene scene = new Scene(root);
            
            currentStage.setScene(scene);
            currentStage.setTitle("Dashboard - BISA GYM");
            currentStage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Error: Gagal kembali ke Dashboard");
            showAlert(AlertType.ERROR, "Error", "Gagal kembali ke Dashboard", 
                     "Tidak dapat memuat halaman dashboard\n\nError: " + e.getMessage());
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Set logged in user information
     */
    public void setUserInfo(String username, String role) {
        this.loggedInUserName = username;
        this.userRole = role;
        
        // Update UI components jika sudah di-initialize
        if (lblNamaPenjaga != null) {
            lblNamaPenjaga.setText(username != null ? username : "Admin");
        }
        if (lblUserRole != null) {
            lblUserRole.setText(role != null ? role : "Operator");
        }
    }

    /**
     * Get current logged in user
     */
    public String getLoggedInUserName() {
        return loggedInUserName;
    }

    /**
     * Get user role
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Update status message
     */
    public void updateStatus(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }

    /**
     * Clean up resources when controller is destroyed
     */
    public void cleanup() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    /**
     * Handle window close event
     */
    public void handleClose() {
        cleanup();
    }

    /**
     * Refresh user interface
     */
    public void refreshUI() {
        // Refresh any dynamic content if needed
        if (lblDateTime != null && timeline != null) {
            // Restart clock if needed
            if (!timeline.getStatus().equals(Animation.Status.RUNNING)) {
                timeline.play();
            }
        }
    }
}