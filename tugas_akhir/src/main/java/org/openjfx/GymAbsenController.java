package org.openjfx;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import static com.mongodb.client.model.Filters.*;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GymAbsenController implements Initializable {

    // FXML Elements - Header & Navigation
    @FXML private Label jam;
    @FXML private ImageView imgkembali;
    
    // FXML Elements - Input Area
    @FXML private TextField txtInputManual;
    @FXML private Button btnCariManual;
    @FXML private Label lblScanStatus;
    
    // FXML Elements - Status Absensi
    @FXML private ImageView imgStatusIcon;
    @FXML private Label lblStatusAbsensi;
    @FXML private Label lblWaktuAbsensi;
    @FXML private Label lblTotalAbsenHariIni;
    
    // FXML Elements - Info Member
    @FXML private Label lblIdMember;
    @FXML private Label lblNama;
    @FXML private Label lblStatusMember;
    @FXML private Label lblMasaBerlaku;
    @FXML private Label lblSisaHari;
    @FXML private Label lblValidityStatus;
    
    // FXML Elements - Statistik
    @FXML private Label lblTotalBulanIni;
    @FXML private Label lblTotalMingguIni;
    @FXML private Label lblTotalKeseluruhan;
    @FXML private Label lblRataRata;
    
    // FXML Elements - Tabel Riwayat
    @FXML private TableView<RiwayatAbsen> tblRiwayatAbsensi;
    @FXML private TableColumn<RiwayatAbsen, String> colNo;
    @FXML private TableColumn<RiwayatAbsen, String> colTanggal;
    @FXML private TableColumn<RiwayatAbsen, String> colWaktu;
    @FXML private TableColumn<RiwayatAbsen, String> colHari;
    @FXML private TableColumn<RiwayatAbsen, String> colStatus;
    @FXML private TableColumn<RiwayatAbsen, String> colKeterangan;

    // Date Formatters
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final SimpleDateFormat dateFormatDB = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormatDB = new SimpleDateFormat("HH:mm:ss");
    
    // MongoDB
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> dataMembersCollection;
    private MongoCollection<Document> dataAbsenMembersCollection;
    
    // Current Member Data
    private String currentMemberId = null;
    private Document currentMemberData = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeDatabase();
        setupClock();
        setupEventHandlers();
        setupTableColumns();
        updateTotalAbsenHariIni();
        resetUI();
    }

    private void initializeDatabase() {
        try {
            String connectionString = "mongodb://localhost:27017";
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase("gym");
            dataMembersCollection = database.getCollection("data_members");
            dataAbsenMembersCollection = database.getCollection("data_absen_members");
            
            System.out.println("✓ Connected to MongoDB successfully");
            
        } catch (Exception e) {
            System.err.println("✗ Error connecting to MongoDB: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                "Tidak dapat terhubung ke database: " + e.getMessage());
        }
    }

    private void setupClock() {
        Timer clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    jam.setText(timeFormat.format(new Date()));
                });
            }
        }, 0, 1000);
    }

    private void setupEventHandlers() {
        btnCariManual.setOnAction(e -> prosesAbsenManual());
        txtInputManual.setOnAction(e -> prosesAbsenManual());
        imgkembali.setOnMouseClicked(e -> kembaliKeMenuMember());
    }

    private void setupTableColumns() {
        colNo.setCellValueFactory(cellData -> cellData.getValue().noProperty());
        colTanggal.setCellValueFactory(cellData -> cellData.getValue().tanggalProperty());
        colWaktu.setCellValueFactory(cellData -> cellData.getValue().waktuProperty());
        colHari.setCellValueFactory(cellData -> cellData.getValue().hariProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colKeterangan.setCellValueFactory(cellData -> cellData.getValue().keteranganProperty());
        
        // Center alignment untuk kolom tertentu
        colNo.setStyle("-fx-alignment: CENTER;");
        colTanggal.setStyle("-fx-alignment: CENTER;");
        colWaktu.setStyle("-fx-alignment: CENTER;");
        colHari.setStyle("-fx-alignment: CENTER;");
        colStatus.setStyle("-fx-alignment: CENTER;");
    }

    private void resetUI() {
        lblScanStatus.setText("Menunggu input...");
        lblScanStatus.setStyle("-fx-text-fill: #7f8c8d;");
        
        lblStatusAbsensi.setText("Belum Absen");
        lblStatusAbsensi.getStyleClass().removeAll("status-success", "status-error");
        lblStatusAbsensi.getStyleClass().add("status-waiting");
        lblWaktuAbsensi.setText("-");
        
        resetMemberInfo();
        tblRiwayatAbsensi.getItems().clear();
        
        currentMemberId = null;
        currentMemberData = null;
    }

    private void prosesAbsenManual() {
        String input = txtInputManual.getText().trim();
        
        if (input.isEmpty()) {
            lblScanStatus.setText("⚠ Masukkan Member ID terlebih dahulu!");
            lblScanStatus.setStyle("-fx-text-fill: #e67e22;");
            showAlert(Alert.AlertType.WARNING, "Peringatan", 
                "Masukkan Member ID atau Nama terlebih dahulu");
            return;
        }
        
        lblScanStatus.setText("⌛ Memproses...");
        lblScanStatus.setStyle("-fx-text-fill: #3498db;");
        
        prosesAbsen(input);
        txtInputManual.clear();
    }

    private void prosesAbsen(String input) {
        try {
            String timestamp = dateTimeFormat.format(new Date());
            String today = dateFormatDB.format(new Date());
            
            // Cari member
            Document member = findMemberByIdOrName(input);
            
            if (member != null) {
                String memberId = member.getString("member_id");
                String nama = member.getString("nama_lengkap");
                
                // Update current member
                currentMemberId = memberId;
                currentMemberData = member;
                
                // Update informasi member
                updateMemberInfo(member);
                
                // Load riwayat absensi member
                loadRiwayatAbsensiMember(memberId);
                
                // Load statistik
                loadStatistikMember(memberId);
                
                // Cek validitas member
                int sisaHari = calculateSisaHari(member);
                
                if (sisaHari < 0) {
                    handleAbsenGagal(timestamp, memberId, nama, "MASA BERLAKU HABIS");
                    lblScanStatus.setText("✗ Member sudah expired!");
                    lblScanStatus.setStyle("-fx-text-fill: #e74c3c;");
                    return;
                }
                
                // Cek apakah sudah absen hari ini
                if (isAlreadyAbsenToday(memberId, today)) {
                    handleAbsenGagal(timestamp, memberId, nama, "SUDAH ABSEN HARI INI");
                    lblScanStatus.setText("⚠ Sudah absen hari ini!");
                    lblScanStatus.setStyle("-fx-text-fill: #f39c12;");
                    showAlert(Alert.AlertType.INFORMATION, "Informasi", 
                        nama + " sudah melakukan absen hari ini");
                } else {
                    // Simpan absensi
                    boolean saved = saveAbsensiToDatabase(member);
                    
                    if (saved) {
                        handleAbsenBerhasil(timestamp, memberId, nama);
                        lblScanStatus.setText("✓ Absen berhasil!");
                        lblScanStatus.setStyle("-fx-text-fill: #27ae60;");
                        
                        // Reload data setelah absen berhasil
                        loadRiwayatAbsensiMember(memberId);
                        loadStatistikMember(memberId);
                        updateTotalAbsenHariIni();
                        
                        showAlert(Alert.AlertType.INFORMATION, "Sukses", 
                            "Absen berhasil untuk " + nama);
                    } else {
                        throw new Exception("Gagal menyimpan data absensi");
                    }
                }
            } else {
                handleMemberTidakDitemukan(timestamp, input);
                lblScanStatus.setText("✗ Member tidak ditemukan!");
                lblScanStatus.setStyle("-fx-text-fill: #e74c3c;");
            }
            
        } catch (Exception e) {
            System.err.println("Error processing attendance: " + e.getMessage());
            e.printStackTrace();
            lblScanStatus.setText("✗ Error: " + e.getMessage());
            lblScanStatus.setStyle("-fx-text-fill: #e74c3c;");
            showAlert(Alert.AlertType.ERROR, "Error", "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private Document findMemberByIdOrName(String input) {
        try {
            input = input.replace("↵", "").trim();
            
            // Cari berdasarkan member_id
            Document byId = dataMembersCollection.find(eq("member_id", input)).first();
            if (byId != null) return byId;
            
            // Cari berdasarkan nama (case insensitive)
            Document regexQuery = new Document("nama_lengkap", 
                new Document("$regex", input).append("$options", "i"));
            Document byName = dataMembersCollection.find(regexQuery).first();
            if (byName != null) return byName;
            
            return null;
            
        } catch (Exception e) {
            System.err.println("Error finding member: " + e.getMessage());
            return null;
        }
    }

    private int calculateSisaHari(Document member) {
        try {
            Object masaBerlakuObj = member.get("tanggal_berlaku_hingga");
            Date masaBerlaku = null;
            
            if (masaBerlakuObj instanceof Date) {
                masaBerlaku = (Date) masaBerlakuObj;
            } else if (masaBerlakuObj instanceof String) {
                String dateStr = ((String) masaBerlakuObj).replace("↵", "").trim();
                masaBerlaku = dateFormatDB.parse(dateStr);
            }
            
            if (masaBerlaku != null) {
                LocalDate berlakuDate = masaBerlaku.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate today = LocalDate.now();
                
                return (int) ChronoUnit.DAYS.between(today, berlakuDate);
            }
            
            return -1;
            
        } catch (Exception e) {
            System.err.println("Error calculating remaining days: " + e.getMessage());
            return -1;
        }
    }

    private void updateMemberInfo(Document member) {
        try {
            // ID Member
            String memberId = member.getString("member_id");
            lblIdMember.setText(memberId != null ? memberId.replace("↵", "") : "-");
            
            // Nama
            String nama = member.getString("nama_lengkap");
            lblNama.setText(nama != null ? nama.replace("↵", "") : "-");
            
            // Status Member
            String status = member.getString("status_keanggotaan");
            lblStatusMember.setText(status != null ? status.replace("↵", "") : "-");
            
            if (status != null && status.contains("Aktif")) {
                lblStatusMember.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblStatusMember.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
            
            // Masa Berlaku
            Object masaBerlakuObj = member.get("tanggal_berlaku_hingga");
            if (masaBerlakuObj instanceof Date) {
                lblMasaBerlaku.setText(dateFormat.format((Date) masaBerlakuObj));
            } else if (masaBerlakuObj instanceof String) {
                String dateStr = ((String) masaBerlakuObj).replace("↵", "").trim();
                try {
                    Date date = dateFormatDB.parse(dateStr);
                    lblMasaBerlaku.setText(dateFormat.format(date));
                } catch (Exception e) {
                    lblMasaBerlaku.setText(dateStr);
                }
            } else {
                lblMasaBerlaku.setText("-");
            }
            
            // Sisa Hari
            int sisaHari = calculateSisaHari(member);
            lblSisaHari.setText(String.valueOf(Math.max(0, sisaHari)));
            
            // Update styling berdasarkan sisa hari
            updateValidityStatus(sisaHari);
            
        } catch (Exception e) {
            System.err.println("Error updating member info: " + e.getMessage());
            resetMemberInfo();
        }
    }

    private void updateValidityStatus(int sisaHari) {
        // Reset semua style classes
        lblValidityStatus.getStyleClass().removeAll(
            "validity-status-normal", "validity-status-warning", "validity-status-danger"
        );
        
        if (sisaHari < 0) {
            lblValidityStatus.setText("⚠ EXPIRED");
            lblValidityStatus.getStyleClass().add("validity-status-danger");
            lblSisaHari.setStyle("-fx-text-fill: #e74c3c;");
        } else if (sisaHari <= 7) {
            lblValidityStatus.setText("⚠ Segera Perpanjang");
            lblValidityStatus.getStyleClass().add("validity-status-danger");
            lblSisaHari.setStyle("-fx-text-fill: #e74c3c;");
        } else if (sisaHari <= 14) {
            lblValidityStatus.setText("⚠ Perlu Diperhatikan");
            lblValidityStatus.getStyleClass().add("validity-status-warning");
            lblSisaHari.setStyle("-fx-text-fill: #f39c12;");
        } else {
            lblValidityStatus.setText("✓ Member Aktif");
            lblValidityStatus.getStyleClass().add("validity-status-normal");
            lblSisaHari.setStyle("-fx-text-fill: #27ae60;");
        }
    }

    private void resetMemberInfo() {
        lblIdMember.setText("-");
        lblNama.setText("-");
        lblStatusMember.setText("-");
        lblMasaBerlaku.setText("-");
        lblSisaHari.setText("0");
        lblValidityStatus.setText("-");
        
        lblTotalBulanIni.setText("0");
        lblTotalMingguIni.setText("0");
        lblTotalKeseluruhan.setText("0");
        lblRataRata.setText("0");
    }

    private void loadRiwayatAbsensiMember(String memberId) {
        try {
            ObservableList<RiwayatAbsen> data = FXCollections.observableArrayList();
            
            // Get data 30 hari terakhir
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            Date thirtyDaysAgo = cal.getTime();
            
            Document query = new Document("kode_member", memberId)
                .append("waktu_absen", new Document("$gte", thirtyDaysAgo));
            
            MongoCursor<Document> cursor = dataAbsenMembersCollection.find(query)
                .sort(new Document("waktu_absen", -1))
                .iterator();
            
            int no = 1;
            SimpleDateFormat sdfHari = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
            
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Date waktuAbsen = doc.getDate("waktu_absen");
                
                String tanggal = dateFormat.format(waktuAbsen);
                String waktu = timeFormatDB.format(waktuAbsen);
                String hari = sdfHari.format(waktuAbsen);
                String status = doc.getString("status");
                String keterangan = "Absensi berhasil";
                
                data.add(new RiwayatAbsen(
                    String.valueOf(no++),
                    tanggal,
                    waktu,
                    hari,
                    status != null ? status : "Hadir",
                    keterangan
                ));
            }
            cursor.close();
            
            tblRiwayatAbsensi.setItems(data);
            
        } catch (Exception e) {
            System.err.println("Error loading attendance history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStatistikMember(String memberId) {
        try {
            // Total Keseluruhan
            long totalAll = dataAbsenMembersCollection.countDocuments(
                eq("kode_member", memberId)
            );
            lblTotalKeseluruhan.setText(String.valueOf(totalAll));
            
            // Total Bulan Ini
            Calendar calMonth = Calendar.getInstance();
            calMonth.set(Calendar.DAY_OF_MONTH, 1);
            calMonth.set(Calendar.HOUR_OF_DAY, 0);
            calMonth.set(Calendar.MINUTE, 0);
            calMonth.set(Calendar.SECOND, 0);
            
            long totalMonth = dataAbsenMembersCollection.countDocuments(
                and(
                    eq("kode_member", memberId),
                    gte("waktu_absen", calMonth.getTime())
                )
            );
            lblTotalBulanIni.setText(String.valueOf(totalMonth));
            
            // Total Minggu Ini
            Calendar calWeek = Calendar.getInstance();
            calWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            calWeek.set(Calendar.HOUR_OF_DAY, 0);
            calWeek.set(Calendar.MINUTE, 0);
            calWeek.set(Calendar.SECOND, 0);
            
            long totalWeek = dataAbsenMembersCollection.countDocuments(
                and(
                    eq("kode_member", memberId),
                    gte("waktu_absen", calWeek.getTime())
                )
            );
            lblTotalMingguIni.setText(String.valueOf(totalWeek));
            
            // Rata-rata per Minggu
            if (totalAll > 0) {
                // Hitung total minggu sejak member join
                Document firstAbsen = dataAbsenMembersCollection
                    .find(eq("kode_member", memberId))
                    .sort(new Document("waktu_absen", 1))
                    .first();
                
                if (firstAbsen != null) {
                    Date firstDate = firstAbsen.getDate("waktu_absen");
                    long daysDiff = TimeUnit.DAYS.convert(
                        new Date().getTime() - firstDate.getTime(), 
                        TimeUnit.MILLISECONDS
                    );
                    long weeksDiff = Math.max(1, daysDiff / 7);
                    
                    double rataRata = (double) totalAll / weeksDiff;
                    lblRataRata.setText(String.format("%.1f", rataRata));
                } else {
                    lblRataRata.setText("0");
                }
            } else {
                lblRataRata.setText("0");
            }
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTotalAbsenHariIni() {
        try {
            String today = dateFormatDB.format(new Date());
            long total = dataAbsenMembersCollection.countDocuments(
                eq("tanggal_absen", today)
            );
            lblTotalAbsenHariIni.setText("Total Absen Hari Ini: " + total);
            
        } catch (Exception e) {
            System.err.println("Error updating today's total: " + e.getMessage());
        }
    }

    private boolean isAlreadyAbsenToday(String memberId, String today) {
        try {
            Document query = new Document("kode_member", memberId)
                .append("tanggal_absen", today);
            
            return dataAbsenMembersCollection.countDocuments(query) > 0;
            
        } catch (Exception e) {
            System.err.println("Error checking existing attendance: " + e.getMessage());
            return false;
        }
    }

    private boolean saveAbsensiToDatabase(Document memberData) {
        try {
            String memberId = memberData.getString("member_id");
            String nama = memberData.getString("nama_lengkap");
            Date now = new Date();

            Document absenDoc = new Document()
                .append("kode_member", memberId)
                .append("nama_member", nama)
                .append("tanggal_absen", dateFormatDB.format(now))
                .append("waktu_absen", now)
                .append("status", "Hadir")
                .append("created_at", now);
            
            if (memberData.containsKey("no_hp")) {
                absenDoc.append("no_hp", memberData.getString("no_hp"));
            }
            if (memberData.containsKey("email")) {
                absenDoc.append("email", memberData.getString("email"));
            }
            
            InsertOneResult result = dataAbsenMembersCollection.insertOne(absenDoc);
            return result.wasAcknowledged();
            
        } catch (Exception e) {
            System.err.println("Error saving attendance: " + e.getMessage());
            return false;
        }
    }

    private void handleAbsenBerhasil(String timestamp, String memberId, String nama) {
        lblStatusAbsensi.setText("✓ ABSEN BERHASIL");
        lblStatusAbsensi.getStyleClass().removeAll("status-waiting", "status-error");
        lblStatusAbsensi.getStyleClass().add("status-success");
        lblWaktuAbsensi.setText(timestamp);
    }

    private void handleAbsenGagal(String timestamp, String memberId, String nama, String reason) {
        lblStatusAbsensi.setText("✗ " + reason);
        lblStatusAbsensi.getStyleClass().removeAll("status-waiting", "status-success");
        lblStatusAbsensi.getStyleClass().add("status-error");
        lblWaktuAbsensi.setText(timestamp);
    }

    private void handleMemberTidakDitemukan(String timestamp, String input) {
        lblStatusAbsensi.setText("✗ MEMBER TIDAK DITEMUKAN");
        lblStatusAbsensi.getStyleClass().removeAll("status-waiting", "status-success");
        lblStatusAbsensi.getStyleClass().add("status-error");
        lblWaktuAbsensi.setText(timestamp);
        resetMemberInfo();
        
        showAlert(Alert.AlertType.ERROR, "Error", 
            "Member dengan ID/Nama '" + input + "' tidak ditemukan dalam database");
    }

    @FXML
    private void kembaliKeMenuMember() {
        try {
            if (mongoClient != null) {
                mongoClient.close();
            }
            
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/menu_member/menu_member.fxml")
            );
            Parent root = loader.load();
            
            Stage stage = (Stage) imgkembali.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Menu Member - Teman Fitness");
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading menu: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Tidak dapat kembali ke menu: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class untuk TableView
    public static class RiwayatAbsen {
        private final SimpleStringProperty no;
        private final SimpleStringProperty tanggal;
        private final SimpleStringProperty waktu;
        private final SimpleStringProperty hari;
        private final SimpleStringProperty status;
        private final SimpleStringProperty keterangan;

        public RiwayatAbsen(String no, String tanggal, String waktu, 
                           String hari, String status, String keterangan) {
            this.no = new SimpleStringProperty(no);
            this.tanggal = new SimpleStringProperty(tanggal);
            this.waktu = new SimpleStringProperty(waktu);
            this.hari = new SimpleStringProperty(hari);
            this.status = new SimpleStringProperty(status);
            this.keterangan = new SimpleStringProperty(keterangan);
        }

        public SimpleStringProperty noProperty() { return no; }
        public SimpleStringProperty tanggalProperty() { return tanggal; }
        public SimpleStringProperty waktuProperty() { return waktu; }
        public SimpleStringProperty hariProperty() { return hari; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty keteranganProperty() { return keterangan; }
    }

    @Override
    protected void finalize() throws Throwable {
        if (mongoClient != null) {
            mongoClient.close();
        }
        super.finalize();
    }
}