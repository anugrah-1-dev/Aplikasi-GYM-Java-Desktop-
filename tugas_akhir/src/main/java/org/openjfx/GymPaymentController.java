package org.openjfx;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GymPaymentController implements Initializable {

    @FXML private RadioButton memberRadio;
    @FXML private RadioButton nonMemberRadio;
    @FXML private ToggleGroup paymentTypeGroup;
    
    @FXML private VBox memberSection;
    @FXML private VBox nonMemberSection;
    @FXML private GridPane memberFormGrid;
    @FXML private VBox nonMemberTypeSection;
    
    @FXML private TextField barcodeField;
    @FXML private ToggleButton scannerToggleButton;
    @FXML private Label scannerStatusLabel;
    
    @FXML private TextField nonMemberIdField;
    @FXML private TextField nonMemberNameField;
    @FXML private DatePicker nonMemberDateField;
    @FXML private ComboBox<String> nonMemberTypeComboBox;
    
    @FXML private DatePicker transactionDateField;
    
    @FXML private Label packageLabel;
    @FXML private ComboBox<String> packageComboBox;
    
    @FXML private TextField memberNameField;
    @FXML private TextField membershipValidityField;
    @FXML private TextField membershipDurationField;
    @FXML private TextField memberPekerjaanField;
    @FXML private Label discountStatusLabel;
    
    @FXML private TextField totalPaymentField;
    @FXML private TextField paidAmountField;
    @FXML private TextField changeAmountField;
    @FXML private Label paymentDetailLabel;
    
    @FXML private Button processButton;
    @FXML private Button resetButton;
    @FXML private Button backButton;
    
    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    
    // Field baru untuk petugas
    @FXML private Label petugasNameLabel;
    @FXML private Label petugasRoleLabel;
    @FXML private TextField petugasUsernameField;
    @FXML private TextField petugasNamaField;
    
    // MongoDB configurations
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "gym";
    private MongoClient mongoClient;
    private MongoDatabase database;
    
    // MongoDB collections
    private MongoCollection<Document> dataMembersCollection;
    private MongoCollection<Document> hargaGymCollection;
    private MongoCollection<Document> transactionsCollection;
    private MongoCollection<Document> loginCollection;
    
    // Data petugas yang sedang login
    private Document currentPetugasData;
    
    // Data paket
    private Map<String, PackageInfo> memberPackages = new LinkedHashMap<>();
    private Map<String, PackageInfo> nonMemberPackages = new LinkedHashMap<>();
    
    // Current member data
    private Document currentMemberData;
    
    // Counter untuk ID non-member
    private AtomicInteger nonMemberCounter = new AtomicInteger(1);
    
    // Variabel untuk scanner
    private StringBuilder barcodeBuffer = new StringBuilder();
    private long lastKeyTime = 0;
    private static final long SCANNER_TIMEOUT_MS = 50;
    private Timeline scannerTimeline;
    private boolean isScannerMode = false;
    
    // Pattern untuk membersihkan barcode
    private static final Pattern VALID_MEMBER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]{3,50}$");
    private static final Pattern EXTRACT_MEMBER_ID_PATTERN = Pattern.compile("([A-Za-z]{2,}-\\d{8}-\\d{6}-\\d{3,4})");
    private static final Pattern COMMON_BARCODE_PATTERNS = Pattern.compile("(?:\\d+[-]?\\d*[-]?)?([A-Za-z]{2,}-\\d{8}-\\d{6}-\\d{3,4})");
    
    // KONSTANTA BIAYA DAN DISKON
    private static final double BIAYA_ADMIN = 30000.0;
    
    // DISKON BERDASARKAN PEKERJAAN
    private static final double DISKON_PELAJAR = 0.10; // 10%
    private static final double DISKON_PNS = 0.10; // 10%
    private static final double DISKON_TNI = 0.10; // 10%
    private static final double DISKON_POLRI = 0.10; // 10%
    private static final double DISKON_BUDAYA_ULAMA = 0.15; // 15%
    private static final double DISKON_GURU = 0.20; // 20%
    
    // ESC/POS Commands untuk printer thermal
    private static final byte[] ESC_INIT = {0x1B, 0x40};
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01};
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] ESC_DOUBLE_ON = {0x1D, 0x21, 0x11};
    private static final byte[] ESC_DOUBLE_OFF = {0x1D, 0x21, 0x00};
    private static final byte[] ESC_CUT = {0x1D, 0x56, 0x00};
    private static final byte[] ESC_FEED = {0x1B, 0x64, 0x02};
    
    // Informasi WiFi
    private static final String WIFI_INFO = 
        "WiFi: BIFVIT24 FITNESS 5G\n" +
        "Pass: MERDEKA45\n" +
        "WiFi: BIFVIT24 FITNESS 4G\n" +
        "Pass: MERDEKA45";
    
    // Class untuk menyimpan informasi paket
    private static class PackageInfo {
        double harga;
        int durasiHari;
        String namaPaket;
        String jenisPaket;
        String tipePaket;
        boolean denganPelatih;
        double biayaPelatihPerBulan;
        
        PackageInfo(String namaPaket, double harga, int durasiHari, String jenisPaket, String tipePaket, boolean denganPelatih, double biayaPelatihPerBulan) {
            this.namaPaket = namaPaket;
            this.harga = harga;
            this.durasiHari = durasiHari;
            this.jenisPaket = jenisPaket;
            this.tipePaket = tipePaket;
            this.denganPelatih = denganPelatih;
            this.biayaPelatihPerBulan = biayaPelatihPerBulan;
        }
        
        public double getHarga() { return harga; }
        public int getDurasiHari() { return durasiHari; }
        public String getNamaPaket() { return namaPaket; }
        public String getJenisPaket() { return jenisPaket; }
        public String getTipePaket() { return tipePaket; }
        public boolean isDenganPelatih() { return denganPelatih; }
        public double getBiayaPelatihPerBulan() { return biayaPelatihPerBulan; }
        
        public double getTotalBiayaPelatih() {
            if (denganPelatih) {
                return harga / 2;
            }
            return 0;
        }
        
        public double getBiayaMemberSaja() {
            if (denganPelatih) {
                return harga / 2;
            }
            return harga;
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing GymPaymentController...");
        
        try {
            initializeDatabase();
            loadCurrentPetugasData(); // Load data petugas yang login
            initializePackages();
            setupEventHandlers();
            setupUI();
            setupBarcodeScanner();
            setupChangeCalculator();
            
            debugDatabaseStructure();
            debugLoadedPackages();
            
            Platform.runLater(() -> {
                setFullScreen();
            });
        } catch (Exception e) {
            System.err.println("Error during initialization: " + e.getMessage());
            e.printStackTrace();
            showAlert("Initialization Error", "Gagal menginisialisasi controller: " + e.getMessage());
        }
    }
    
    /**
     * Method untuk memuat data petugas yang sedang login dari koleksi login
     */
    private void loadCurrentPetugasData() {
        try {
            System.out.println("🔍 Loading current petugas data...");
            
            // Ambil data login terbaru (asumsi data login terbaru adalah yang sedang aktif)
            Document latestLogin = loginCollection.find()
                .sort(new Document("login_time", -1))
                .first();
            
            if (latestLogin != null) {
                currentPetugasData = latestLogin;
                String username = latestLogin.getString("username");
                String role = latestLogin.getString("role");
                String nama = latestLogin.getString("nama");
                
                System.out.println("✅ Petugas data loaded:");
                System.out.println("   Username: " + username);
                System.out.println("   Role: " + role);
                System.out.println("   Nama: " + nama);
                
                // Update UI dengan data petugas
                Platform.runLater(() -> {
                    petugasUsernameField.setText(username != null ? username : "-");
                    petugasNamaField.setText(nama != null ? nama : "-");
                    petugasNameLabel.setText(nama != null ? nama : "-");
                    petugasRoleLabel.setText(role != null ? role : "-");
                });
            } else {
                System.out.println("⚠️ No login data found, using default values");
                setDefaultPetugasData();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error loading petugas data: " + e.getMessage());
            setDefaultPetugasData();
        }
    }
    
    /**
     * Set default data petugas jika tidak ada data login
     */
    private void setDefaultPetugasData() {
        Platform.runLater(() -> {
            petugasUsernameField.setText("admin");
            petugasNamaField.setText("Administrator");
            petugasNameLabel.setText("Administrator");
            petugasRoleLabel.setText("Admin");
        });
    }
    
    private void setFullScreen() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setMaximized(true);
            System.out.println("Full screen mode activated");
        } catch (Exception e) {
            System.err.println("Error setting full screen: " + e.getMessage());
        }
    }
    
    private void connectToDatabase() {
        try {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
            System.out.println("Connected to MongoDB successfully");
        } catch (Exception e) {
            System.err.println("Error connecting to MongoDB: " + e.getMessage());
            showAlert("Database Error", "Gagal terhubung ke database: " + e.getMessage());
        }
    }

    private MongoCollection<Document> getCollection(String collectionName) {
        if (database == null) {
            connectToDatabase();
        }
        return database.getCollection(collectionName);
    }
    
    private void initializeDatabase() {
        try {
            connectToDatabase();
            dataMembersCollection = getCollection("data_members");
            hargaGymCollection = getCollection("harga_gym");
            transactionsCollection = getCollection("transactions");
            loginCollection = getCollection("login"); // Tambahan koleksi login
            
            System.out.println("Database collections initialized successfully");
            
            initializeNonMemberCounter();
            
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            showAlert("Database Error", "Gagal menginisialisasi database: " + e.getMessage());
        }
    }
    
    private void debugDatabaseStructure() {
        try {
            System.out.println("=== DATABASE STRUCTURE DEBUG ===");
            
            System.out.println("Harga Gym Documents:");
            for (Document doc : hargaGymCollection.find()) {
                System.out.println("  " + doc.toJson());
            }
            
            Document sampleMember = dataMembersCollection.find().first();
            if (sampleMember != null) {
                System.out.println("Sample member fields:");
                for (String key : sampleMember.keySet()) {
                    System.out.println("  " + key + ": " + sampleMember.get(key));
                }
            }
            
            // Debug login data
            System.out.println("Login Documents:");
            for (Document doc : loginCollection.find().limit(5)) {
                System.out.println("  " + doc.toJson());
            }
            
            System.out.println("=== END DEBUG ===");
        } catch (Exception e) {
            System.err.println("Error in debug: " + e.getMessage());
        }
    }
    
    private void debugLoadedPackages() {
        System.out.println("\n=== DEBUG LOADED PACKAGES ===");
        
        System.out.println("MEMBER PACKAGES:");
        memberPackages.forEach((key, value) -> {
            System.out.println("  " + key + " - Rp " + value.getHarga() + " - " + value.getDurasiHari() + " hari - Pelatih: " + value.isDenganPelatih());
        });
        
        System.out.println("NON-MEMBER PACKAGES:");
        nonMemberPackages.forEach((key, value) -> {
            System.out.println("  " + key + " - Rp " + value.getHarga() + " - " + value.getDurasiHari() + " hari");
        });
        
        System.out.println("=== END DEBUG ===\n");
    }
    
    private void initializeNonMemberCounter() {
        try {
            Document lastNonMember = transactionsCollection
                .find(Filters.eq("jenis_pembayaran", "non_member_regular"))
                .sort(new Document("_id", -1))
                .first();
            
            if (lastNonMember != null) {
                String lastId = lastNonMember.getString("non_member_id");
                if (lastId != null && lastId.startsWith("NM")) {
                    try {
                        String numberPart = lastId.substring(2);
                        int lastNumber = Integer.parseInt(numberPart);
                        nonMemberCounter.set(lastNumber + 1);
                    } catch (NumberFormatException e) {
                        nonMemberCounter.set((int) System.currentTimeMillis());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing non-member counter: " + e.getMessage());
            nonMemberCounter.set((int) System.currentTimeMillis());
        }
    }
    
    private void initializePackages() {
        loadHargaFromDatabase();
        transactionDateField.setValue(LocalDate.now());
        nonMemberDateField.setValue(LocalDate.now());
        
        barcodeField.setPromptText("Scan barcode atau masukkan member_id member");
        nonMemberNameField.setPromptText("Masukkan nama lengkap");
        nonMemberIdField.setPromptText("Auto-generated");
        nonMemberIdField.setEditable(false);
        memberPekerjaanField.setPromptText("Pekerjaan member");
        memberPekerjaanField.setEditable(false);
        
        nonMemberTypeComboBox.getItems().clear();
        nonMemberTypeComboBox.getItems().add("Regular");
        nonMemberTypeComboBox.setValue("Regular");
        
        generateNonMemberId();
    }
    
    private void generateNonMemberId() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String idNumber = timestamp.substring(timestamp.length() - 8);
            String id = "NM" + idNumber;
            nonMemberIdField.setText(id);
        } catch (Exception e) {
            String id = "NM" + nonMemberCounter.getAndIncrement();
            nonMemberIdField.setText(id);
        }
    }
    
    private int getMemberAge(Document member) {
        try {
            String tanggalLahirStr = member.getString("tanggal_lahir");
            if (tanggalLahirStr != null && !tanggalLahirStr.isEmpty()) {
                LocalDate tanggalLahir = LocalDate.parse(tanggalLahirStr);
                LocalDate today = LocalDate.now();
                int age = java.time.Period.between(tanggalLahir, today).getYears();
                System.out.println("🎂 Calculated member age: " + age + " years (DOB: " + tanggalLahirStr + ")");
                return age;
            } else {
                System.out.println("⚠️ No date of birth found, assuming adult member");
                return 18;
            }
        } catch (Exception e) {
            System.err.println("❌ Error calculating member age: " + e.getMessage());
            return 18;
        }
    }
    
    // ==================== METHOD UNTUK MENGHITUNG DISKON ====================
    
    /**
     * Menghitung persentase diskon berdasarkan pekerjaan member
     * @param pekerjaan pekerjaan dari database
     * @return persentase diskon (0.0 - 1.0)
     */
    private double getDiscountPercentage(String pekerjaan) {
        if (pekerjaan == null || pekerjaan.trim().isEmpty()) {
            return 0.0;
        }
        
        String pekerjaanLower = pekerjaan.toLowerCase().trim();
        
        // Pelajar/Mahasiswa - 10%
        if (pekerjaanLower.contains("pelajar") || pekerjaanLower.contains("mahasiswa") || 
            pekerjaanLower.contains("siswa") || pekerjaanLower.contains("student")) {
            System.out.println("💰 Diskon Pelajar: 10%");
            return DISKON_PELAJAR;
        }
        
        // PNS - 10%
        if (pekerjaanLower.contains("pns") || pekerjaanLower.contains("pegawai negeri sipil")) {
            System.out.println("💰 Diskon PNS: 10%");
            return DISKON_PNS;
        }
        
        // TNI - 10%
        if (pekerjaanLower.contains("tni") || pekerjaanLower.contains("tentara") || 
            pekerjaanLower.contains("militer")) {
            System.out.println("💰 Diskon TNI: 10%");
            return DISKON_TNI;
        }
        
        // POLRI - 10%
        if (pekerjaanLower.contains("polri") || pekerjaanLower.contains("polisi") || 
            pekerjaanLower.contains("kepolisian")) {
            System.out.println("💰 Diskon POLRI: 10%");
            return DISKON_POLRI;
        }
        
        // Guru - 20%
        if (pekerjaanLower.contains("guru") || pekerjaanLower.contains("pengajar") || 
            pekerjaanLower.contains("dosen") || pekerjaanLower.contains("teacher")) {
            System.out.println("💰 Diskon Guru: 20%");
            return DISKON_GURU;
        }
        
        // Pelaku Budaya/Ulama - 15%
        if (pekerjaanLower.contains("pelaku budaya / ulama") || pekerjaanLower.contains("seniman") || 
            pekerjaanLower.contains("ustadz") || pekerjaanLower.contains("kyai") || 
            pekerjaanLower.contains("pendeta") || pekerjaanLower.contains("pastor") ||
            pekerjaanLower.contains("biksu")) {
            System.out.println("💰 Diskon Pelaku Budaya/Ulama: 15%");
            return DISKON_BUDAYA_ULAMA;
        }
        
        System.out.println("💰 Tidak ada diskon untuk pekerjaan: " + pekerjaan);
        return 0.0;
    }
    
    /**
     * Mendapatkan label diskon untuk ditampilkan di UI
     */
    private String getDiscountLabel(double percentage) {
        if (percentage == 0.0) {
            return "Tidak ada diskon";
        }
        int percent = (int) (percentage * 100);
        return String.format("✓ Diskon %d%% (berdasarkan pekerjaan)", percent);
    }
    
    /**
     * Menghitung total pembayaran dengan diskon dan biaya admin
     */
    private PaymentCalculation calculatePaymentWithDiscount(double hargaPaket, String pekerjaan) {
        double discountPercentage = getDiscountPercentage(pekerjaan);
        double discountAmount = hargaPaket * discountPercentage;
        double hargaSetelahDiskon = hargaPaket - discountAmount;
        double totalWithAdmin = hargaSetelahDiskon + BIAYA_ADMIN;
        
        System.out.println("💵 PERHITUNGAN PEMBAYARAN:");
        System.out.println("   Harga Paket: Rp " + String.format("%,d", (int) hargaPaket));
        System.out.println("   Diskon (" + (int)(discountPercentage * 100) + "%): Rp " + String.format("%,d", (int) discountAmount));
        System.out.println("   Harga Setelah Diskon: Rp " + String.format("%,d", (int) hargaSetelahDiskon));
        System.out.println("   Biaya Admin: Rp " + String.format("%,d", (int) BIAYA_ADMIN));
        System.out.println("   TOTAL: Rp " + String.format("%,d", (int) totalWithAdmin));
        
        return new PaymentCalculation(
            hargaPaket,
            discountPercentage,
            discountAmount,
            hargaSetelahDiskon,
            BIAYA_ADMIN,
            totalWithAdmin
        );
    }
    
    /**
     * Class untuk menyimpan hasil perhitungan pembayaran
     */
    private static class PaymentCalculation {
        double hargaAsli;
        double persenDiskon;
        double nominalDiskon;
        double hargaSetelahDiskon;
        double biayaAdmin;
        double totalAkhir;
        
        PaymentCalculation(double hargaAsli, double persenDiskon, double nominalDiskon, 
                          double hargaSetelahDiskon, double biayaAdmin, double totalAkhir) {
            this.hargaAsli = hargaAsli;
            this.persenDiskon = persenDiskon;
            this.nominalDiskon = nominalDiskon;
            this.hargaSetelahDiskon = hargaSetelahDiskon;
            this.biayaAdmin = biayaAdmin;
            this.totalAkhir = totalAkhir;
        }
    }
    
    // ==================== END METHOD DISKON ====================
    
    private void loadHargaFromDatabase() {
        try {
            System.out.println("📊 Loading packages from database...");
            
            Document memberHarga = hargaGymCollection.find(Filters.eq("jenis", "member")).first();
            if (memberHarga != null) {
                System.out.println("✅ Found member pricing in database");
                
                double biayaPelatihPerBulan = 200000;
                
                if (memberHarga.containsKey("1_bulan")) {
                    double harga1Bulan = getDoubleValue(memberHarga.get("1_bulan"));
                    memberPackages.put("1 Bulan", new PackageInfo("1 Bulan", harga1Bulan, 30, "bulan", "member", false, biayaPelatihPerBulan));
                    System.out.println("✅ 1 Bulan: Rp " + harga1Bulan + " (30 hari)");
                } else if (memberHarga.containsKey("i_bulan")) {
                    double harga1Bulan = getDoubleValue(memberHarga.get("i_bulan"));
                    memberPackages.put("1 Bulan", new PackageInfo("1 Bulan", harga1Bulan, 30, "bulan", "member", false, biayaPelatihPerBulan));
                    System.out.println("✅ 1 Bulan (i_bulan): Rp " + harga1Bulan + " (30 hari)");
                }
                
                if (memberHarga.containsKey("3_bulan")) {
                    double harga3Bulan = getDoubleValue(memberHarga.get("3_bulan"));
                    memberPackages.put("3 Bulan", new PackageInfo("3 Bulan", harga3Bulan, 90, "bulan", "member", false, biayaPelatihPerBulan));
                    System.out.println("✅ 3 Bulan: Rp " + harga3Bulan + " (90 hari)");
                }
                
                if (memberHarga.containsKey("6_bulan")) {
                    double harga6Bulan = getDoubleValue(memberHarga.get("6_bulan"));
                    memberPackages.put("6 Bulan", new PackageInfo("6 Bulan", harga6Bulan, 180, "bulan", "member", false, biayaPelatihPerBulan));
                    System.out.println("✅ 6 Bulan: Rp " + harga6Bulan + " (180 hari)");
                }
                
                if (memberHarga.containsKey("1_bulan")) {
                    double harga1Bulan = getDoubleValue(memberHarga.get("1_bulan"));
                    double totalWithTrainer1 = harga1Bulan * 2;
                    memberPackages.put("1 Bulan + Pelatih", new PackageInfo("1 Bulan + Pelatih", totalWithTrainer1, 30, "bulan", "member", true, biayaPelatihPerBulan));
                    System.out.println("✅ 1 Bulan + Pelatih: Rp " + totalWithTrainer1 + " (30 hari)");
                } else if (memberHarga.containsKey("i_bulan")) {
                    double harga1Bulan = getDoubleValue(memberHarga.get("i_bulan"));
                    double totalWithTrainer1 = harga1Bulan * 2;
                    memberPackages.put("1 Bulan + Pelatih", new PackageInfo("1 Bulan + Pelatih", totalWithTrainer1, 30, "bulan", "member", true, biayaPelatihPerBulan));
                    System.out.println("✅ 1 Bulan + Pelatih (i_bulan): Rp " + totalWithTrainer1 + " (30 hari)");
                }
                
                if (memberHarga.containsKey("3_bulan")) {
                    double harga3Bulan = getDoubleValue(memberHarga.get("3_bulan"));
                    double totalWithTrainer3 = harga3Bulan * 2;
                    memberPackages.put("3 Bulan + Pelatih", new PackageInfo("3 Bulan + Pelatih", totalWithTrainer3, 90, "bulan", "member", true, biayaPelatihPerBulan));
                    System.out.println("✅ 3 Bulan + Pelatih: Rp " + totalWithTrainer3 + " (90 hari)");
                }
                
                if (memberHarga.containsKey("6_bulan")) {
                    double harga6Bulan = getDoubleValue(memberHarga.get("6_bulan"));
                    double totalWithTrainer6 = harga6Bulan * 2;
                    memberPackages.put("6 Bulan + Pelatih", new PackageInfo("6 Bulan + Pelatih", totalWithTrainer6, 180, "bulan", "member", true, biayaPelatihPerBulan));
                    System.out.println("✅ 6 Bulan + Pelatih: Rp " + totalWithTrainer6 + " (180 hari)");
                }
                
            } else {
                System.out.println("⚠️ No member pricing found, using defaults based on image data");
                setDefaultMemberPackages();
            }
            
            Document nonMemberRegular = hargaGymCollection.find(Filters.eq("jenis", "non_member_regular")).first();
            if (nonMemberRegular != null) {
                System.out.println("✅ Found non-member regular pricing in database");
                if (nonMemberRegular.containsKey("harian")) {
                    double hargaHarian = getDoubleValue(nonMemberRegular.get("harian"));
                    nonMemberPackages.put("Harian", new PackageInfo("Harian", hargaHarian, 1, "hari", "non_member_regular", false, 0));
                    System.out.println("✅ Non-Member Regular Harian: Rp " + hargaHarian + " (1 hari)");
                }
            } else {
                System.out.println("⚠️ No non-member pricing found, using defaults");
                setDefaultNonMemberPackages();
            }
            
            System.out.println("📦 Total member packages loaded: " + memberPackages.size());
            System.out.println("📦 Total non-member packages loaded: " + nonMemberPackages.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error loading harga from database: " + e.getMessage());
            setDefaultMemberPackages();
            setDefaultNonMemberPackages();
        }
    }
    
    private double getDoubleValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    private void setDefaultMemberPackages() {
        double biayaPelatihPerBulan = 200000;
        
        System.out.println("⚠️ Using default member packages based on image data");
        
        memberPackages.put("1 Bulan", new PackageInfo("1 Bulan", 250000.0, 30, "bulan", "member", false, biayaPelatihPerBulan));
        memberPackages.put("3 Bulan", new PackageInfo("3 Bulan", 675000.0, 90, "bulan", "member", false, biayaPelatihPerBulan));
        memberPackages.put("6 Bulan", new PackageInfo("6 Bulan", 1200000.0, 180, "bulan", "member", false, biayaPelatihPerBulan));
        
        memberPackages.put("1 Bulan + Pelatih", new PackageInfo("1 Bulan + Pelatih", 250000.0 * 2, 30, "bulan", "member", true, biayaPelatihPerBulan));
        memberPackages.put("3 Bulan + Pelatih", new PackageInfo("3 Bulan + Pelatih", 675000.0 * 2, 90, "bulan", "member", true, biayaPelatihPerBulan));
        memberPackages.put("6 Bulan + Pelatih", new PackageInfo("6 Bulan + Pelatih", 1200000.0 * 2, 180, "bulan", "member", true, biayaPelatihPerBulan));
    }
    
    private void setDefaultNonMemberPackages() {
        nonMemberPackages.put("Harian", new PackageInfo("Harian", 50000.0, 1, "hari", "non_member_regular", false, 0));
        System.out.println("⚠️ Using default non-member regular packages");
    }
    
    private void setupEventHandlers() {
        paymentTypeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == memberRadio) {
                showMemberSection();
            } else if (newValue == nonMemberRadio) {
                showNonMemberSection();
            }
        });
        
        packageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            calculateTotalPayment();
        });
        
        nonMemberTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateNonMemberPackages();
            calculateTotalPayment();
        });
        
        processButton.setOnAction(event -> processPayment());
        resetButton.setOnAction(event -> resetForm());
        backButton.setOnAction(event -> goBackToDashboard());
        
        setupClock();
    }
    
    private void enablePackageSelection() {
        if (currentMemberData == null) {
            packageComboBox.setDisable(true);
            return;
        }
        
        int currentAge = getMemberAge(currentMemberData);
        System.out.println("📅 Member age: " + currentAge + " years");
        
        packageComboBox.getItems().clear();
        
        if (currentAge < 15) {
            System.out.println("👦 Member di bawah 15 tahun - HANYA paket dengan pelatih");
            for (String packageName : memberPackages.keySet()) {
                if (packageName.contains("+ Pelatih")) {
                    packageComboBox.getItems().add(packageName);
                }
            }
        } else {
            System.out.println("👨 Member 15 tahun ke atas - semua paket available");
            packageComboBox.getItems().addAll(memberPackages.keySet());
        }
        
        packageComboBox.setDisable(false);
        
        if (!packageComboBox.getItems().isEmpty()) {
            packageComboBox.setValue(packageComboBox.getItems().get(0));
        }
        
        calculateTotalPayment();
    }
    
    private void setupBarcodeScanner() {
        System.out.println("🔄 Setting up barcode scanner...");
        
        barcodeBuffer.setLength(0);
        isScannerMode = false;
        
        barcodeField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleBarcodeScannerInput);
        
        barcodeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (scannerToggleButton.isSelected() && newValue != null && !newValue.isEmpty()) {
                if (scannerTimeline != null) {
                    scannerTimeline.stop();
                }
                
                scannerTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
                    if (scannerToggleButton.isSelected()) {
                        String scannedBarcode = barcodeField.getText().trim();
                        if (!scannedBarcode.isEmpty()) {
                            processScannerInput(scannedBarcode);
                        }
                    }
                }));
                scannerTimeline.play();
            }
        });
        
        barcodeField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String barcode = barcodeField.getText().trim();
                if (!barcode.isEmpty()) {
                    processScannerInput(barcode);
                    event.consume();
                }
            }
        });
        
        scannerToggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            isScannerMode = newValue;
            scannerStatusLabel.setText(newValue ? "🔴 Scanner AKTIF" : "⚪ Scanner non-aktif");
        });
        
        System.out.println("✅ Barcode scanner setup completed");
    }
    
    private void processScannerInput(String scannedBarcode) {
        String cleanedBarcode = cleanAndExtractMemberId(scannedBarcode);
        
        if (isValidMemberId(cleanedBarcode)) {
            Platform.runLater(() -> {
                barcodeField.setText(cleanedBarcode);
                barcodeField.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
            });
            
            searchMemberByBarcode(cleanedBarcode);
        } else {
            Platform.runLater(() -> {
                barcodeField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showAlert("Barcode Tidak Valid", "Format barcode tidak valid: " + cleanedBarcode);
            });
        }
    }
    
    private String cleanAndExtractMemberId(String rawBarcode) {
        if (rawBarcode == null || rawBarcode.trim().isEmpty()) {
            return rawBarcode;
        }
        
        String cleanedBarcode = rawBarcode.trim();
        System.out.println("🔍 Raw barcode received: " + cleanedBarcode);
        
        if (isValidMemberId(cleanedBarcode)) {
            System.out.println("✅ Barcode already valid: " + cleanedBarcode);
            return cleanedBarcode;
        }
        
        java.util.regex.Matcher matcher = EXTRACT_MEMBER_ID_PATTERN.matcher(cleanedBarcode);
        if (matcher.find()) {
            String extractedId = matcher.group(1);
            System.out.println("✅ Extracted member_id using pattern: " + extractedId);
            return extractedId;
        }
        
        matcher = COMMON_BARCODE_PATTERNS.matcher(cleanedBarcode);
        if (matcher.find()) {
            String extractedId = matcher.group(1);
            System.out.println("✅ Extracted member_id from common pattern: " + extractedId);
            return extractedId;
        }
        
        System.out.println("🧹 Using original (trimmed) barcode: " + cleanedBarcode);
        return cleanedBarcode;
    }
    
    private boolean isValidMemberId(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            return false;
        }
        
        String cleanId = memberId.trim();
        
        if (cleanId.length() < 3 || cleanId.length() > 50) {
            return false;
        }
        
        if (!cleanId.matches("^[A-Za-z0-9\\-]+$")) {
            return false;
        }
        
        return true;
    }
    
    private void handleBarcodeScannerInput(KeyEvent event) {
        if (!scannerToggleButton.isSelected()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastKeyTime > SCANNER_TIMEOUT_MS) {
            if (barcodeBuffer.length() > 0) {
                System.out.println("🔄 Buffer reset");
            }
            barcodeBuffer.setLength(0);
            isScannerMode = true;
        }
        
        lastKeyTime = currentTime;
        
        if (event.getCode() == KeyCode.ENTER) {
            String scannedBarcode = barcodeBuffer.toString();
            if (!scannedBarcode.isEmpty()) {
                processScannerInput(scannedBarcode);
            }
            barcodeBuffer.setLength(0);
            event.consume();
        }
    }
    
    private void updateNonMemberPackages() {
        packageComboBox.getItems().clear();
        
        String selectedType = nonMemberTypeComboBox.getValue();
        if ("Regular".equals(selectedType)) {
            packageComboBox.getItems().addAll(nonMemberPackages.keySet());
        }
        
        if (!packageComboBox.getItems().isEmpty()) {
            packageComboBox.setValue(packageComboBox.getItems().get(0));
        }
    }
    
    private void goBackToDashboard() {
        try {
            closeDatabaseConnection();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_transaksi/menu_transaksi.fxml"));
            Parent root = loader.load();
            
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setMaximized(true);
            currentStage.show();
            
        } catch (Exception e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            showAlert("Error", "Gagal kembali ke dashboard: " + e.getMessage());
        }
    }
    
    private void searchMemberByBarcode(String barcode) {
        try {
            String cleanBarcode = barcode.trim();
            Document member = dataMembersCollection.find(Filters.eq("member_id", cleanBarcode)).first();
            
            if (member != null) {
                currentMemberData = member;
                displayMemberInfo(member);
                enablePackageSelection();
                
                Platform.runLater(() -> {
                    barcodeField.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
                });
            } else {
                clearMemberInfo();
                Platform.runLater(() -> {
                    barcodeField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showAlert("Member Tidak Ditemukan", "Member dengan ID '" + cleanBarcode + "' tidak ditemukan.");
                });
            }
        } catch (Exception e) {
            System.err.println("Error searching member: " + e.getMessage());
            clearMemberInfo();
        }
    }
    
    private String getMemberName(Document member) {
        if (member.getString("nama_lengkap") != null) return member.getString("nama_lengkap");
        if (member.getString("nama") != null) return member.getString("nama");
        return "Nama tidak tersedia";
    }
    
    private void displayMemberInfo(Document member) {
        try {
            String namaLengkap = getMemberName(member);
            String tanggalBerlakuHinggaStr = member.getString("tanggal_berlaku_hingga_str");
            String durasiMember = member.getString("durasi_member");
            String pekerjaan = member.getString("pekerjaan");
            
            memberNameField.setText(namaLengkap != null ? namaLengkap : "-");
            membershipValidityField.setText(tanggalBerlakuHinggaStr != null ? formatDate(tanggalBerlakuHinggaStr) : "-");
            membershipDurationField.setText(durasiMember != null ? durasiMember : "-");
            memberPekerjaanField.setText(pekerjaan != null && !pekerjaan.isEmpty() ? pekerjaan : "Tidak ada data");
            
            // Update label diskon
            double discountPercentage = getDiscountPercentage(pekerjaan);
            String discountLabel = getDiscountLabel(discountPercentage);
            discountStatusLabel.setText(discountLabel);
            
            // Ubah warna label berdasarkan diskon
            if (discountPercentage > 0) {
                discountStatusLabel.setStyle("-fx-background-color: #d4edda; -fx-border-color: #28a745; -fx-text-fill: #155724;");
            } else {
                discountStatusLabel.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #f39c12; -fx-text-fill: #856404;");
            }
            
        } catch (Exception e) {
            System.err.println("Error displaying member info: " + e.getMessage());
        }
    }
    
    private String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    private void clearMemberInfo() {
        memberNameField.clear();
        membershipValidityField.clear();
        membershipDurationField.clear();
        memberPekerjaanField.clear();
        paymentDetailLabel.setText("");
        packageComboBox.setDisable(true);
        packageComboBox.getSelectionModel().clearSelection();
        totalPaymentField.clear();
        paidAmountField.clear();
        changeAmountField.clear();
        discountStatusLabel.setText("Tidak ada diskon");
        discountStatusLabel.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #f39c12; -fx-text-fill: #856404;");
        currentMemberData = null;
    }
    
    private void setupUI() {
        showMemberSection();
    }
    
    private void showMemberSection() {
        memberSection.setVisible(true);
        memberSection.setManaged(true);
        nonMemberSection.setVisible(false);
        nonMemberSection.setManaged(false);
        nonMemberTypeSection.setVisible(false);
        nonMemberTypeSection.setManaged(false);
        if (memberFormGrid != null) {
            memberFormGrid.setVisible(true);
            memberFormGrid.setManaged(true);
        }
        
        packageLabel.setText("Pilih Paket Member");
        packageComboBox.getItems().clear();
        packageComboBox.setDisable(true);
        
        resetNonMemberFields();
        calculateTotalPayment();
    }
    
    private void showNonMemberSection() {
        memberSection.setVisible(false);
        memberSection.setManaged(false);
        nonMemberSection.setVisible(true);
        nonMemberSection.setManaged(true);
        nonMemberTypeSection.setVisible(true);
        nonMemberTypeSection.setManaged(true);
        if (memberFormGrid != null) {
            memberFormGrid.setVisible(false);
            memberFormGrid.setManaged(false);
        }
        
        packageLabel.setText("Pilih Paket Non-Member");
        updateNonMemberPackages();
        packageComboBox.setDisable(false);
        
        resetMemberFields();
        generateNonMemberId();
        calculateTotalPayment();
    }
    
    private void setupChangeCalculator() {
        paidAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            calculateChange();
        });
        
        paidAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                paidAmountField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        
        paidAmountField.setPromptText("Masukkan jumlah uang yang dibayar");
        changeAmountField.setPromptText("Kembalian akan dihitung otomatis");
        changeAmountField.setEditable(false);
        
        System.out.println("✅ Change calculator setup completed");
    }
    
    private void calculateChange() {
        try {
            String totalText = totalPaymentField.getText();
            String paidText = paidAmountField.getText();
            
            if (totalText == null || totalText.isEmpty() || totalText.equals("")) {
                changeAmountField.setText("");
                changeAmountField.setStyle("");
                return;
            }
            
            if (paidText == null || paidText.isEmpty()) {
                changeAmountField.setText("");
                changeAmountField.setStyle("");
                return;
            }
            
            String totalClean = totalText.replace("Rp", "").replace(",", "").replace(".", "").trim();
            double total = Double.parseDouble(totalClean);
            
            double paid = Double.parseDouble(paidText);
            double change = paid - total;
            
            if (change < 0) {
                changeAmountField.setText(String.format("KURANG: Rp %,d", (int) Math.abs(change)));
                changeAmountField.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else if (change == 0) {
                changeAmountField.setText("PAS (Tidak ada kembalian)");
                changeAmountField.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                changeAmountField.setText(String.format("Rp %,d", (int) change));
                changeAmountField.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
            }
            
        } catch (NumberFormatException e) {
            changeAmountField.setText("");
            changeAmountField.setStyle("");
        } catch (Exception e) {
            System.err.println("Error calculating change: " + e.getMessage());
            changeAmountField.setText("");
            changeAmountField.setStyle("");
        }
    }
    
    private void calculateTotalPayment() {
        try {
            String selectedPackage = packageComboBox.getValue();
            double total = 0.0;
            String detail = "Pilih paket untuk melihat detail";
            
            if (memberRadio.isSelected()) {
                if (selectedPackage != null && memberPackages.containsKey(selectedPackage) && currentMemberData != null) {
                    PackageInfo packageInfo = memberPackages.get(selectedPackage);
                    double hargaPaket = packageInfo.getHarga();
                    String pekerjaan = currentMemberData.getString("pekerjaan");
                    
                    // Hitung dengan diskon dan biaya admin
                    PaymentCalculation calculation = calculatePaymentWithDiscount(hargaPaket, pekerjaan);
                    total = calculation.totalAkhir;
                    
                    if (packageInfo.isDenganPelatih()) {
                        int durasiBulan = packageInfo.getDurasiHari() / 30;
                        double biayaPelatih = packageInfo.getTotalBiayaPelatih();
                        double biayaMember = packageInfo.getBiayaMemberSaja();
                        
                        // Hitung diskon untuk member saja (tidak untuk pelatih)
                        PaymentCalculation memberCalc = calculatePaymentWithDiscount(biayaMember, pekerjaan);
                        PaymentCalculation pelatihCalc = calculatePaymentWithDiscount(biayaPelatih, null); // Tidak ada diskon untuk pelatih
                        
                        double totalSebelumAdmin = memberCalc.hargaSetelahDiskon + pelatihCalc.hargaSetelahDiskon;
                        total = totalSebelumAdmin + BIAYA_ADMIN;
                        
                        detail = String.format(
                            "Paket %s (%d hari)\n" +
                            "Biaya Member: Rp %,d\n" +
                            "Diskon Member (%d%%): -Rp %,d\n" +
                            "Biaya Pelatih (%d bulan): Rp %,d\n" +
                            "Subtotal: Rp %,d\n" +
                            "Biaya Admin: Rp %,d\n" +
                            "TOTAL: Rp %,d",
                            selectedPackage.replace(" + Pelatih", ""),
                            packageInfo.getDurasiHari(),
                            (int) biayaMember,
                            (int) (memberCalc.persenDiskon * 100),
                            (int) memberCalc.nominalDiskon,
                            durasiBulan,
                            (int) biayaPelatih,
                            (int) totalSebelumAdmin,
                            (int) BIAYA_ADMIN,
                            (int) total
                        );
                    } else {
                        detail = String.format(
                            "Paket %s (%d hari)\n" +
                            "Harga Normal: Rp %,d\n" +
                            "Diskon (%d%%): -Rp %,d\n" +
                            "Harga Setelah Diskon: Rp %,d\n" +
                            "Biaya Admin: Rp %,d\n" +
                            "TOTAL: Rp %,d",
                            selectedPackage,
                            packageInfo.getDurasiHari(),
                            (int) calculation.hargaAsli,
                            (int) (calculation.persenDiskon * 100),
                            (int) calculation.nominalDiskon,
                            (int) calculation.hargaSetelahDiskon,
                            (int) BIAYA_ADMIN,
                            (int) total
                        );
                    }
                }
            } else if (nonMemberRadio.isSelected()) {
                if (selectedPackage != null) {
                    PackageInfo packageInfo = nonMemberPackages.get(selectedPackage);
                    total = packageInfo.getHarga();
                    detail = String.format("Paket %s - Regular\nTotal: Rp %,d", selectedPackage, (int) total);
                }
            }
            
            if (total > 0) {
                totalPaymentField.setText(String.format("Rp %,d", (int) total));
                paymentDetailLabel.setText(detail);
                calculateChange();
            } else {
                totalPaymentField.setText("");
                paidAmountField.setText("");
                changeAmountField.setText("");
                paymentDetailLabel.setText("Pilih paket untuk melihat detail");
            }
        } catch (Exception e) {
            System.err.println("Error calculating total payment: " + e.getMessage());
            totalPaymentField.setText("");
            paidAmountField.setText("");
            changeAmountField.setText("");
        }
    }
    
    private void processPayment() {
        if (!validateForm()) {
            return;
        }
        
        if (!validatePayment()) {
            return;
        }
        
        if (memberRadio.isSelected()) {
            processMemberPayment();
        } else {
            processNonMemberPayment();
        }
    }
    
    private boolean validatePayment() {
        try {
            String paidText = paidAmountField.getText();
            
            if (paidText == null || paidText.isEmpty()) {
                showAlert("Error Pembayaran", "Harap masukkan jumlah uang yang dibayar pelanggan!");
                paidAmountField.requestFocus();
                return false;
            }
            
            String totalText = totalPaymentField.getText();
            String totalClean = totalText.replace("Rp", "").replace(",", "").replace(".", "").trim();
            double total = Double.parseDouble(totalClean);
            
            double paid = Double.parseDouble(paidText);
            
            if (paid < total) {
                double kurang = total - paid;
                showAlert("Uang Tidak Cukup", 
                    String.format("Uang yang dibayar tidak cukup!\n\n" +
                                "Total: Rp %,d\n" +
                                "Dibayar: Rp %,d\n" +
                                "Kurang: Rp %,d\n\n" +
                                "Harap masukkan jumlah yang tepat.",
                                (int) total, (int) paid, (int) kurang));
                paidAmountField.requestFocus();
                return false;
            }
            
            return true;
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Format jumlah uang tidak valid!");
            paidAmountField.requestFocus();
            return false;
        } catch (Exception e) {
            showAlert("Error", "Terjadi kesalahan saat validasi pembayaran: " + e.getMessage());
            return false;
        }
    }
    
    private boolean validateForm() {
        if (transactionDateField.getValue() == null) {
            showAlert("Error", "Harap pilih tanggal transaksi");
            return false;
        }
        
        if (packageComboBox.getValue() == null) {
            showAlert("Error", "Harap pilih paket");
            return false;
        }
        
        if (memberRadio.isSelected()) {
            if (barcodeField.getText().isEmpty()) {
                showAlert("Error", "Harap masukkan member_id member");
                return false;
            }
            if (currentMemberData == null) {
                showAlert("Error", "Data member tidak valid");
                return false;
            }
        } else {
            if (nonMemberNameField.getText().isEmpty()) {
                showAlert("Error", "Harap masukkan nama non-member");
                return false;
            }
            if (nonMemberDateField.getValue() == null) {
                showAlert("Error", "Harap pilih tanggal masuk");
                return false;
            }
        }
        
        return true;
    }
    
    private void processMemberPayment() {
        try {
            String memberId = barcodeField.getText();
            String packageName = packageComboBox.getValue();
            PackageInfo packageInfo = memberPackages.get(packageName);
            double hargaPaket = packageInfo.getHarga();
            int durasiHari = packageInfo.getDurasiHari();
            boolean denganPelatih = packageInfo.isDenganPelatih();
            double biayaPelatih = packageInfo.getTotalBiayaPelatih();
            double biayaMember = packageInfo.getBiayaMemberSaja();
            
            String pekerjaan = currentMemberData.getString("pekerjaan");
            
            // Hitung dengan diskon dan biaya admin
            PaymentCalculation calculation = calculatePaymentWithDiscount(hargaPaket, pekerjaan);
            double amount = calculation.totalAkhir;
            
            LocalDate transactionDate = transactionDateField.getValue();
            LocalDate newExpiryDate = calculateNewExpiryDate(packageName);
            
            double paidAmount = Double.parseDouble(paidAmountField.getText());
            double change = paidAmount - amount;
            
            updateMemberData(newExpiryDate, durasiHari, packageName, denganPelatih);
            saveTransactionToDatabase(memberId, packageName, hargaPaket, amount, durasiHari, denganPelatih, 
                biayaMember, biayaPelatih, transactionDate, newExpiryDate, paidAmount, change,
                pekerjaan, calculation.persenDiskon, calculation.nominalDiskon, BIAYA_ADMIN);
            
            String confirmationMessage;
            if (denganPelatih) {
                int durasiBulan = durasiHari / 30;
                confirmationMessage = String.format(
                    "Pembayaran untuk %s berhasil!\n\n" +
                    "Member ID: %s\n" +
                    "Pekerjaan: %s\n" +
                    "Paket: %s (%d hari)\n" +
                    "Biaya Member: Rp %,d\n" +
                    "Diskon (%d%%): -Rp %,d\n" +
                    "Biaya Pelatih (%d bulan): Rp %,d\n" +
                    "Biaya Admin: Rp %,d\n" +
                    "Total: Rp %,d\n" +
                    "Dibayar: Rp %,d\n" +
                    "Kembalian: Rp %,d\n\n" +
                    "Berlaku hingga: %s",
                    getMemberName(currentMemberData),
                    memberId,
                    pekerjaan != null ? pekerjaan : "-",
                    packageName.replace(" + Pelatih", ""),
                    durasiHari,
                    (int) biayaMember,
                    (int) (calculation.persenDiskon * 100),
                    (int) calculation.nominalDiskon,
                    durasiBulan,
                    (int) biayaPelatih,
                    (int) BIAYA_ADMIN,
                    (int) amount,
                    (int) paidAmount,
                    (int) change,
                    newExpiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
            } else {
                confirmationMessage = String.format(
                    "Pembayaran untuk %s berhasil!\n\n" +
                    "Member ID: %s\n" +
                    "Pekerjaan: %s\n" +
                    "Paket: %s (%d hari)\n" +
                    "Harga Normal: Rp %,d\n" +
                    "Diskon (%d%%): -Rp %,d\n" +
                    "Harga Setelah Diskon: Rp %,d\n" +
                    "Biaya Admin: Rp %,d\n" +
                    "Total: Rp %,d\n" +
                    "Dibayar: Rp %,d\n" +
                    "Kembalian: Rp %,d\n\n" +
                    "Berlaku hingga: %s",
                    getMemberName(currentMemberData),
                    memberId,
                    pekerjaan != null ? pekerjaan : "-",
                    packageName,
                    durasiHari,
                    (int) calculation.hargaAsli,
                    (int) (calculation.persenDiskon * 100),
                    (int) calculation.nominalDiskon,
                    (int) calculation.hargaSetelahDiskon,
                    (int) BIAYA_ADMIN,
                    (int) amount,
                    (int) paidAmount,
                    (int) change,
                    newExpiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
            }
            
            showConfirmationWithPrintOption("Pembayaran Member Berhasil", confirmationMessage, true, 
                memberId, packageName, hargaPaket, amount, durasiHari, denganPelatih, biayaMember, biayaPelatih, 
                transactionDate, newExpiryDate, paidAmount, change, pekerjaan, calculation.persenDiskon, 
                calculation.nominalDiskon, BIAYA_ADMIN);
            
        } catch (Exception e) {
            System.err.println("Error processing member payment: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal memproses pembayaran member: " + e.getMessage());
        }
    }
    
    private LocalDate calculateNewExpiryDate(String packageName) {
        LocalDate startDate = transactionDateField.getValue();
        PackageInfo packageInfo = memberPackages.get(packageName);
        int durasiHari = packageInfo.getDurasiHari();
        
        System.out.println("📅 Calculating expiry date for package: " + packageName);
        System.out.println("📅 Start date: " + startDate);
        System.out.println("📅 Duration days: " + durasiHari);
        
        if (currentMemberData != null) {
            String currentExpiryStr = currentMemberData.getString("tanggal_berlaku_hingga_str");
            String status = currentMemberData.getString("status_keanggotaan");
            
            System.out.println("📅 Current expiry from DB: " + currentExpiryStr);
            System.out.println("📅 Current status: " + status);
            
            if (currentExpiryStr != null && status != null && status.equalsIgnoreCase("Aktif")) {
                try {
                    LocalDate currentExpiry = LocalDate.parse(currentExpiryStr);
                    System.out.println("📅 Parsed current expiry: " + currentExpiry);
                    
                    if (currentExpiry.isAfter(startDate) || currentExpiry.isEqual(startDate)) {
                        LocalDate newExpiry = currentExpiry.plusDays(durasiHari);
                        System.out.println("📅 Extended from current expiry: " + newExpiry);
                        return newExpiry;
                    } else {
                        System.out.println("📅 Current membership expired, starting from today");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error parsing current expiry date: " + e.getMessage());
                }
            } else {
                System.out.println("📅 No active membership found, starting from today");
            }
        }
        
        LocalDate newExpiry = startDate.plusDays(durasiHari);
        System.out.println("📅 New expiry from start date: " + newExpiry);
        return newExpiry;
    }
    
    private void updateMemberData(LocalDate newExpiryDate, int durasiHari, String packageName, boolean denganPelatih) {
        try {
            String memberId = currentMemberData.getString("member_id");
            
            System.out.println("🔄 Updating member data for: " + memberId);
            System.out.println("🔄 New expiry date: " + newExpiryDate);
            System.out.println("🔄 Duration: " + durasiHari + " days");
            System.out.println("🔄 Package: " + packageName);
            System.out.println("🔄 With Trainer: " + denganPelatih);
            
            Document updateDoc = new Document("$set", new Document()
                .append("tanggal_berlaku_hingga_str", newExpiryDate.toString())
                .append("status_keanggotaan", "Aktif")
                .append("durasi_member", packageName)
                .append("durasi_hari", durasiHari)
                .append("dengan_pelatih", denganPelatih)
                .append("last_updated", new Date()));
            
            System.out.println("📝 Update document: " + updateDoc.toJson());
            
            var result = dataMembersCollection.updateOne(
                Filters.eq("member_id", memberId),
                updateDoc
            );
            
            System.out.println("✅ Member data updated successfully");
            System.out.println("✅ Modified count: " + result.getModifiedCount());
            
        } catch (Exception e) {
            System.err.println("❌ Error updating member data: " + e.getMessage());
            throw new RuntimeException("Gagal update data member: " + e.getMessage());
        }
    }
    
    private void saveTransactionToDatabase(String memberId, String packageName, double hargaAsli, double totalBayar,
                                         int durasiHari, boolean denganPelatih, double biayaMember, double biayaPelatih,
                                         LocalDate transactionDate, LocalDate expiryDate, double paidAmount, double change,
                                         String pekerjaan, double persenDiskon, double nominalDiskon, double biayaAdmin) {
        try {
            // Ambil data petugas
            String petugasUsername = currentPetugasData != null ? currentPetugasData.getString("username") : "unknown";
            String petugasNama = currentPetugasData != null ? currentPetugasData.getString("nama") : "Unknown";
            
            Document transaction = new Document()
                .append("_id", new ObjectId())
                .append("member_id", memberId)
                .append("nama_member", getMemberName(currentMemberData))
                .append("pekerjaan", pekerjaan)
                .append("jenis_pembayaran", "member")
                .append("paket", packageName)
                .append("durasi_hari", durasiHari)
                .append("dengan_pelatih", denganPelatih)
                .append("biaya_member", biayaMember)
                .append("biaya_pelatih", biayaPelatih)
                .append("harga_asli", hargaAsli)
                .append("persen_diskon", persenDiskon)
                .append("nominal_diskon", nominalDiskon)
                .append("biaya_admin", biayaAdmin)
                .append("jumlah", totalBayar)
                .append("jumlah_dibayar", paidAmount)
                .append("kembalian", change)
                .append("tanggal_transaksi", transactionDate.toString())
                .append("tanggal_berlaku_hingga", expiryDate.toString())
                .append("petugas_username", petugasUsername) // Tambahan field petugas
                .append("petugas_nama", petugasNama) // Tambahan field petugas
                .append("created_at", new Date())
                .append("updated_at", new Date());
            
            System.out.println("💾 Saving transaction to database...");
            System.out.println("📝 Transaction data: " + transaction.toJson());
            
            transactionsCollection.insertOne(transaction);
            
            System.out.println("✅ Transaction saved successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error saving transaction: " + e.getMessage());
            throw new RuntimeException("Gagal menyimpan transaksi: " + e.getMessage());
        }
    }
    
    private void processNonMemberPayment() {
        try {
            String name = nonMemberNameField.getText().trim();
            String id = nonMemberIdField.getText();
            String packageName = packageComboBox.getValue();
            String tipe = nonMemberTypeComboBox.getValue();
            double amount = 0.0;
            String jenisPembayaran = "non_member_regular";
            
            PackageInfo packageInfo = nonMemberPackages.get(packageName);
            amount = packageInfo.getHarga();
            
            LocalDate entryDate = nonMemberDateField.getValue();
            LocalDate transactionDate = transactionDateField.getValue();
            
            double paidAmount = Double.parseDouble(paidAmountField.getText());
            double change = paidAmount - amount;
            
            saveNonMemberTransactionToDatabase(id, name, packageName, tipe, jenisPembayaran, amount, entryDate, transactionDate, paidAmount, change);
            
            String confirmationMessage = String.format("Pembayaran untuk %s berhasil!\n\n" +
                            "ID: %s\n" +
                            "Paket: %s - %s\n" +
                            "Total: Rp %,d\n" +
                            "Dibayar: Rp %,d\n" +
                            "Kembalian: Rp %,d\n\n" +
                            "Tanggal masuk: %s",
                    name,
                    id,
                    packageName,
                    tipe,
                    (int) amount,
                    (int) paidAmount,
                    (int) change,
                    entryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            
            showConfirmationWithPrintOption("Pembayaran Non-Member Berhasil", confirmationMessage, false,
                id, name, packageName, tipe, amount, entryDate, transactionDate, paidAmount, change);
            
        } catch (Exception e) {
            System.err.println("Error processing non-member payment: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal memproses pembayaran non-member: " + e.getMessage());
        }
    }
    
    private void saveNonMemberTransactionToDatabase(String id, String name, String packageName, String tipe, 
                                                  String jenisPembayaran, double amount, LocalDate entryDate, 
                                                  LocalDate transactionDate, double paidAmount, double change) {
        try {
            // Ambil data petugas
            String petugasUsername = currentPetugasData != null ? currentPetugasData.getString("username") : "unknown";
            String petugasNama = currentPetugasData != null ? currentPetugasData.getString("nama") : "Unknown";
            
            Document transaction = new Document()
                .append("_id", new ObjectId())
                .append("non_member_id", id)
                .append("nama_non_member", name)
                .append("jenis_pembayaran", jenisPembayaran)
                .append("paket", packageName)
                .append("tipe", tipe)
                .append("jumlah", amount)
                .append("jumlah_dibayar", paidAmount)
                .append("kembalian", change)
                .append("tanggal_masuk", entryDate.toString())
                .append("tanggal_transaksi", transactionDate.toString())
                .append("petugas_username", petugasUsername) // Tambahan field petugas
                .append("petugas_nama", petugasNama) // Tambahan field petugas
                .append("status", "selesai")
                .append("created_at", new Date())
                .append("updated_at", new Date());
            
            transactionsCollection.insertOne(transaction);
        } catch (Exception e) {
            System.err.println("Error saving non-member transaction: " + e.getMessage());
            throw new RuntimeException("Gagal menyimpan transaksi non-member: " + e.getMessage());
        }
    }

    // ==================== PRINT RECEIPT METHODS ====================

    private void showConfirmationWithPrintOption(String title, String message, boolean isMember, Object... transactionData) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText("Transaksi Berhasil!");
            alert.setContentText(message + "\n\nApakah Anda ingin mencetak struk?");
            
            ButtonType buttonTypePrint = new ButtonType("Cetak Struk");
            ButtonType buttonTypeNoPrint = new ButtonType("Tidak");
            
            alert.getButtonTypes().setAll(buttonTypePrint, buttonTypeNoPrint);
            
            alert.showAndWait().ifPresent(response -> {
                if (response == buttonTypePrint) {
                    if (isMember) {
                        printMemberReceipt(transactionData);
                    } else {
                        printNonMemberReceipt(transactionData);
                    }
                }
                resetForm();
            });
        });
    }

    private void printMemberReceipt(Object... data) {
        try {
            String memberId = (String) data[0];
            String packageName = (String) data[1];
            double hargaAsli = (double) data[2];
            double totalBayar = (double) data[3];
            int durasiHari = (int) data[4];
            boolean denganPelatih = (boolean) data[5];
            double biayaMember = (double) data[6];
            double biayaPelatih = (double) data[7];
            LocalDate transactionDate = (LocalDate) data[8];
            LocalDate expiryDate = (LocalDate) data[9];
            double paidAmount = (double) data[10];
            double change = (double) data[11];
            String pekerjaan = (String) data[12];
            double persenDiskon = (double) data[13];
            double nominalDiskon = (double) data[14];
            double biayaAdmin = (double) data[15];
            
            StringBuilder receipt = new StringBuilder();
            
            // ========== HEADER ==========
            receipt.append(centerText("BISA GYM CENTER"));
            receipt.append(centerText("STRUK PEMBAYARAN MEMBER"));
            
            String formattedDateTime = java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"));
            receipt.append("Tanggal: ").append(formattedDateTime).append("\n\n");
            
            // Info Member
            receipt.append("Member ID: ").append(memberId).append("\n");
            receipt.append("Nama: ").append(shortenText(getMemberName(currentMemberData), 20)).append("\n");
            if (pekerjaan != null && !pekerjaan.isEmpty()) {
                receipt.append("Pekerjaan: ").append(shortenText(pekerjaan, 18)).append("\n");
            }
            receipt.append("\n");
            
            // Detail Paket
            receipt.append("Paket: ").append(packageName.replace(" + Pelatih", "")).append("\n");
            receipt.append("Durasi: ").append(durasiHari).append(" hari\n\n");
            
            // Biaya Breakdown
            if (denganPelatih) {
                receipt.append("Biaya Member:\n");
                receipt.append(formatCurrencyLine("", biayaMember));
                receipt.append("Biaya Pelatih:\n");
                receipt.append(formatCurrencyLine("", biayaPelatih));
            } else {
                receipt.append("Harga Normal:\n");
                receipt.append(formatCurrencyLine("", hargaAsli));
            }
            
            // Diskon
            if (nominalDiskon > 0) {
                receipt.append("Diskon (").append((int)(persenDiskon * 100)).append("%):\n");
                receipt.append(formatCurrencyLine("", -nominalDiskon));
            }
            
            // Biaya Admin
            receipt.append("Biaya Admin:\n");
            receipt.append(formatCurrencyLine("", biayaAdmin));
            
            receipt.append("\n");
            receipt.append("TOTAL:\n");
            receipt.append(formatCurrencyLine("", totalBayar));
            receipt.append("Dibayar:\n");
            receipt.append(formatCurrencyLine("", paidAmount));
            receipt.append("Kembalian:\n");
            receipt.append(formatCurrencyLine("", change));
            
            // Garis pemisah
            receipt.append("\n");
            receipt.append("----------------------------\n");
            
            // Masa berlaku
            receipt.append("Berlaku hingga:\n");
            receipt.append(expiryDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))).append("\n\n");
            
            // Info Petugas
            String petugasNama = currentPetugasData != null ? currentPetugasData.getString("nama") : "Unknown";
            receipt.append("Petugas: ").append(petugasNama).append("\n\n");
            
            // WiFi Info
            receipt.append(centerText("--- AKSES WiFi ---"));
            receipt.append("BIFVIT24 FITNESS 5G\n");
            receipt.append("Password: MERDEKA45\n");
            receipt.append("BIFVIT24 FITNESS 4G\n");
            receipt.append("Password: MERDEKA45\n\n");
            
            // Footer
            receipt.append(centerText("Terima Kasih"));
            receipt.append(centerText("Selamat Berolahraga!"));
            
            System.out.println("📄 Struk Member yang akan dicetak:");
            System.out.println(receipt.toString());
            
            printToThermalPrinter(receipt.toString());
            
        } catch (Exception e) {
            System.err.println("Error printing member receipt: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error Cetak", "Gagal mencetak struk: " + e.getMessage());
        }
    }

    private void printNonMemberReceipt(Object... data) {
        try {
            String id = (String) data[0];
            String name = (String) data[1];
            String packageName = (String) data[2];
            String tipe = (String) data[3];
            double amount = (double) data[4];
            LocalDate entryDate = (LocalDate) data[5];
            LocalDate transactionDate = (LocalDate) data[6];
            double paidAmount = (double) data[7];
            double change = (double) data[8];
            
            StringBuilder receipt = new StringBuilder();
            
            // ========== HEADER ==========
            receipt.append(centerText("BISA GYM CENTER"));
            receipt.append(centerText("STRUK PEMBAYARAN NON-MEMBER"));
            
            String formattedDateTime = java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"));
            receipt.append("Tanggal: ").append(formattedDateTime).append("\n\n");
            
            // Info Non-Member
            receipt.append("ID: ").append(id).append("\n");
            receipt.append("Nama: ").append(shortenText(name, 20)).append("\n\n");
            
            // Detail Paket
            receipt.append("Paket: ").append(packageName).append("\n");
            receipt.append("Tipe: ").append(tipe).append("\n\n");
            
            // Biaya
            receipt.append("Biaya:\n");
            receipt.append(formatCurrencyLine("", amount));
            receipt.append("Dibayar:\n");
            receipt.append(formatCurrencyLine("", paidAmount));
            receipt.append("Kembalian:\n");
            receipt.append(formatCurrencyLine("", change));
            
            // Garis pemisah
            receipt.append("\n");
            receipt.append("----------------------------\n");
            
            // Tanggal masuk
            receipt.append("Tanggal Masuk:\n");
            receipt.append(entryDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))).append("\n\n");
            
            // Info Petugas
            String petugasNama = currentPetugasData != null ? currentPetugasData.getString("nama") : "Unknown";
            receipt.append("Petugas: ").append(petugasNama).append("\n\n");
            
            // WiFi Info
            receipt.append(centerText("--- AKSES WiFi ---"));
            receipt.append("BIFVIT24 FITNESS 5G\n");
            receipt.append("Password: MERDEKA45\n");
            receipt.append("BIFVIT24 FITNESS 4G\n");
            receipt.append("Password: MERDEKA45\n\n");
            
            // Footer
            receipt.append(centerText("Terima Kasih"));
            receipt.append(centerText("Selamat Berolahraga!"));
            
            System.out.println("📄 Struk Non-Member yang akan dicetak:");
            System.out.println(receipt.toString());
            
            printToThermalPrinter(receipt.toString());
            
        } catch (Exception e) {
            System.err.println("Error printing non-member receipt: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error Cetak", "Gagal mencetak struk: " + e.getMessage());
        }
    }

    // ========== METHOD HELPER UNTUK STRUK ==========

    private String shortenText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatCurrencyLine(String label, double amount) {
        String amountStr = String.format("Rp %,d", (int) amount);
        
        if (label == null || label.isEmpty()) {
            return "  " + amountStr + "\n";
        } else {
            int totalLength = 28;
            int labelLength = label.length();
            int amountLength = amountStr.length();
            int spaceNeeded = totalLength - labelLength - amountLength;
            
            if (spaceNeeded > 0) {
                return label + " ".repeat(spaceNeeded) + amountStr + "\n";
            } else {
                return label + "\n" + " ".repeat(totalLength - amountLength) + amountStr + "\n";
            }
        }
    }

    private String centerText(String text) {
        int lineWidth = 32;
        String cleanText = text.trim();
        int textLength = cleanText.length();
        
        if (textLength >= lineWidth) {
            return cleanText + "\n";
        }
        
        int padding = (lineWidth - textLength) / 2;
        if (padding > 0) {
            return " ".repeat(padding) + cleanText + "\n";
        }
        return cleanText + "\n";
    }

    private void printToThermalPrinter(String content) {
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            AtomicReference<PrintService> thermalPrinterRef = new AtomicReference<>();
            
            System.out.println("🖨️ Mencari printer thermal...");
            
            String[] preferredPrinters = {"POS-58", "POS58", "thermal", "receipt", "58mm", "xprinter", "zjiang", "pos"};
            
            for (PrintService service : printServices) {
                String printerName = service.getName().toLowerCase();
                System.out.println("   Printer tersedia: " + service.getName());
                
                for (String preferred : preferredPrinters) {
                    if (printerName.contains(preferred.toLowerCase())) {
                        thermalPrinterRef.set(service);
                        System.out.println("✅ Printer thermal ditemukan: " + service.getName());
                        break;
                    }
                }
                if (thermalPrinterRef.get() != null) break;
            }
            
            if (thermalPrinterRef.get() == null) {
                thermalPrinterRef.set(PrintServiceLookup.lookupDefaultPrintService());
                if (thermalPrinterRef.get() != null) {
                    System.out.println("⚠️ Menggunakan default printer: " + thermalPrinterRef.get().getName());
                } else {
                    throw new Exception("Tidak ada printer yang tersedia!");
                }
            }
            
            byte[] receiptBytes = buildESCPOSReceipt(content);
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(receiptBytes, flavor, null);
            
            PrintService selectedPrinter = thermalPrinterRef.get();
            DocPrintJob printJob = selectedPrinter.createPrintJob();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            
            printJob.print(doc, attributes);
            
            System.out.println("✅ Struk berhasil dicetak!");
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Cetak Berhasil");
                alert.setContentText("Struk berhasil dicetak ke printer " + selectedPrinter.getName());
                alert.showAndWait();
            });
            
        } catch (Exception e) {
            System.err.println("❌ Error printing to thermal printer: " + e.getMessage());
            e.printStackTrace();
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error Cetak");
                alert.setHeaderText("Gagal mencetak struk");
                alert.setContentText("Error: " + e.getMessage() + "\n\nPastikan printer thermal sudah terhubung dan siap digunakan.");
                alert.showAndWait();
            });
        }
    }

    private byte[] buildESCPOSReceipt(String content) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            
            baos.write(ESC_INIT);
            baos.write(new byte[]{0x1B, 0x74, 0x17});
            
            baos.write(ESC_ALIGN_CENTER);
            baos.write(ESC_BOLD_ON);
            baos.write(ESC_DOUBLE_ON);
            baos.write("BISA GYM CENTER\n".getBytes(StandardCharsets.UTF_8));
            baos.write(ESC_BOLD_OFF);
            baos.write(ESC_DOUBLE_OFF);
            
            baos.write(ESC_BOLD_ON);
            baos.write("STRUK PEMBAYARAN MEMBER\n".getBytes(StandardCharsets.UTF_8));
            baos.write(ESC_BOLD_OFF);
            
            baos.write(ESC_ALIGN_LEFT);
            baos.write(ESC_BOLD_OFF);
            baos.write(ESC_DOUBLE_OFF);
            
            baos.write(new byte[]{0x1B, 0x33, 0x10});
            
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.contains("BISA GYM CENTER") || line.contains("STRUK PEMBAYARAN")) {
                    continue;
                }
                
                if (line.contains("TOTAL:") || line.contains("Dibayar:") || line.contains("Kembalian:")) {
                    baos.write(ESC_BOLD_ON);
                }
                
                baos.write(line.getBytes(StandardCharsets.UTF_8));
                baos.write("\n".getBytes(StandardCharsets.UTF_8));
                
                if (line.contains("TOTAL:") || line.contains("Dibayar:") || line.contains("Kembalian:")) {
                    baos.write(ESC_BOLD_OFF);
                }
            }
            
            baos.write(ESC_FEED);
            baos.write(ESC_CUT);
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            System.err.println("Error building ESC/POS receipt: " + e.getMessage());
            
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                baos.write(ESC_INIT);
                baos.write(ESC_ALIGN_LEFT);
                baos.write(content.getBytes(StandardCharsets.UTF_8));
                baos.write(ESC_FEED);
                baos.write(ESC_CUT);
                return baos.toByteArray();
            } catch (Exception ex) {
                return content.getBytes(StandardCharsets.UTF_8);
            }
        }
    }
    
    private void resetForm() {
        try {
            barcodeField.clear();
            barcodeField.setStyle("");
            nonMemberNameField.clear();
            memberNameField.clear();
            membershipValidityField.clear();
            membershipDurationField.clear();
            memberPekerjaanField.clear();
            totalPaymentField.clear();
            paidAmountField.clear();
            changeAmountField.clear();
            paymentDetailLabel.setText("");
            discountStatusLabel.setText("Tidak ada diskon");
            discountStatusLabel.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #f39c12; -fx-text-fill: #856404;");
            
            packageComboBox.getSelectionModel().clearSelection();
            nonMemberTypeComboBox.setValue("Regular");
            transactionDateField.setValue(LocalDate.now());
            nonMemberDateField.setValue(LocalDate.now());
            
            currentMemberData = null;
            barcodeBuffer.setLength(0);
            isScannerMode = false;
            
            if (memberRadio.isSelected()) {
                packageComboBox.setDisable(true);
            }
            
            generateNonMemberId();
            
        } catch (Exception e) {
            System.err.println("Error resetting form: " + e.getMessage());
        }
    }
    
    private void resetMemberFields() {
        barcodeField.clear();
        barcodeField.setStyle("");
        memberNameField.clear();
        membershipValidityField.clear();
        membershipDurationField.clear();
        memberPekerjaanField.clear();
        discountStatusLabel.setText("Tidak ada diskon");
        discountStatusLabel.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #f39c12; -fx-text-fill: #856404;");
        currentMemberData = null;
        barcodeBuffer.setLength(0);
        isScannerMode = false;
    }
    
    private void resetNonMemberFields() {
        nonMemberNameField.clear();
        nonMemberDateField.setValue(LocalDate.now());
        generateNonMemberId();
    }
    
    private void showAlert(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }
    
    private void setupClock() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            String time = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String date = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
            clockLabel.setText(time);
            if (dateLabel != null) {
                dateLabel.setText(date);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    public void closeDatabaseConnection() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                System.out.println("Database connection closed");
            } catch (Exception e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}