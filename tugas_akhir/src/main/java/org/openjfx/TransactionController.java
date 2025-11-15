package org.openjfx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

// IMPORT APACHE POI YANG BENAR - GUNAKAN INI:
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row; // ← INI DARI APACHE POI!
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.bson.Document;
import org.bson.types.ObjectId;

// JANGAN GUNAKAN: import com.lowagie.text.Row;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class TransactionController implements Initializable {
    
    // Header Elements - SESUAI FXML
    @FXML private Label timeLabel;
    @FXML private Label guardNameLabel;
    
    // Statistics Labels - SESUAI FXML
    @FXML private Label totalTransactionsLabel;
    @FXML private Label revenueTodayLabel;
    @FXML private Label pendingTransactionsLabel;
    @FXML private Label averageTransactionLabel;
    
    // Filter Elements - SESUAI FXML
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> dateFilter;
    @FXML private TextField searchField;
    @FXML private Label filterStatusLabel;
    
    // Table Elements - SESUAI FXML
    @FXML private TableView<TransactionData> transactionTable;
    @FXML private TableColumn<TransactionData, String> idColumn;
    @FXML private TableColumn<TransactionData, String> dateColumn;
    @FXML private TableColumn<TransactionData, String> customerColumn;
    @FXML private TableColumn<TransactionData, String> productColumn;
    @FXML private TableColumn<TransactionData, Integer> quantityColumn;
    @FXML private TableColumn<TransactionData, String> totalColumn;
    @FXML private TableColumn<TransactionData, String> statusColumn;
    
    private ObservableList<TransactionData> allTransactions;
    private ObservableList<TransactionData> filteredTransactions;
    private MongoDatabase database;
    private NumberFormat currencyFormat;
    private Timeline clockTimeline;
    
    // Session tracking for shift recap
    private LocalDateTime shiftStartTime;
    private String currentGuardName = "penjaga";
    private String currentGuardId = null;
    
    // Static method untuk set guard info dari login
    private static String loggedInGuardName = null;
    private static String loggedInGuardId = null;
    
    public static void setLoggedInGuard(String guardName, String guardId) {
        loggedInGuardName = guardName;
        loggedInGuardId = guardId;
        System.out.println("✅ Guard info set: " + guardName + " (" + guardId + ")");
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ TransactionController initialized");
        
        try {
            // Initialize database connection
            database = connectToDatabase();
            if (database == null) {
                showError("Koneksi Database Gagal", "Tidak dapat terhubung ke database MongoDB");
                return;
            }
            
            currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            
            // Initialize collections
            allTransactions = FXCollections.observableArrayList();
            filteredTransactions = FXCollections.observableArrayList();
            
            // Set guard info from login
            if (loggedInGuardName != null) {
                currentGuardName = loggedInGuardName;
                currentGuardId = loggedInGuardId;
            }
            
            // Set shift start time
            shiftStartTime = LocalDateTime.now();
            
            // Update guard name label
            guardNameLabel.setText("Penjaga: " + currentGuardName);
            
            // Record shift start
            recordShiftStart();
            
            // Setup clock
            setupClock();
            
            // Setup table columns
            setupTableColumns();
            
            // Setup filters
            setupFilters();
            
            // Setup event handlers
            setupEventHandlers();
            
            // Load transactions
            loadAllTransactions();
            
            // Initial statistics update
            updateStatistics();
            
            System.out.println("✅ Controller initialized successfully");
            System.out.println("👤 Current guard: " + currentGuardName);
            System.out.println("📊 Total transaksi dimuat: " + allTransactions.size());
            
        } catch (Exception e) {
            System.err.println("❌ Initialization error: " + e.getMessage());
            e.printStackTrace();
            showError("Error Inisialisasi", "Gagal menginisialisasi controller: " + e.getMessage());
        }
    }
    
    /**
     * Record shift start to database
     */
    private void recordShiftStart() {
        try {
            MongoCollection<Document> shiftCollection = database.getCollection("active_shifts");
            
            // Check if there's already an active shift for this guard
            Document existingShift = shiftCollection.find(
                new Document("guard_name", currentGuardName)
                    .append("shift_end", null)
            ).first();
            
            if (existingShift == null) {
                Document shiftDoc = new Document()
                    .append("guard_name", currentGuardName)
                    .append("guard_id", currentGuardId)
                    .append("shift_start", Date.from(shiftStartTime.atZone(ZoneId.systemDefault()).toInstant()))
                    .append("shift_end", null)
                    .append("status", "active");
                
                shiftCollection.insertOne(shiftDoc);
                System.out.println("✅ Shift start recorded for " + currentGuardName);
            } else {
                // Use existing shift start time
                Date existingStart = existingShift.getDate("shift_start");
                shiftStartTime = existingStart.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                System.out.println("✅ Continuing existing shift for " + currentGuardName);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error recording shift start: " + e.getMessage());
        }
    }
    
    /**
     * Connect to MongoDB database
     */
    private MongoDatabase connectToDatabase() {
        try {
            // Try to get from MenuTransaksiController if available
            try {
                MongoDatabase db = MenuTransaksiController.MongoDBConnector.getDatabase();
                if (db != null) {
                    System.out.println("✅ Connected via MenuTransaksiController");
                    return db;
                }
            } catch (Exception e) {
                System.out.println("⚠️ MenuTransaksiController not available, using direct connection");
            }
            
            // Direct connection to MongoDB
            String connectionString = "mongodb://localhost:27017";
            MongoClient mongoClient = MongoClients.create(connectionString);
            MongoDatabase db = mongoClient.getDatabase("gym");
            
            System.out.println("✅ Connected to MongoDB directly");
            return db;
            
        } catch (Exception e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private void setupClock() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeLabel.setText(LocalDateTime.now().format(timeFormatter));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }
    
    private void setupTableColumns() {
        // Setup cell value factories - SESUAI DENGAN NAMA DI FXML
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        customerColumn.setCellValueFactory(cellData -> cellData.getValue().customerProperty());
        productColumn.setCellValueFactory(cellData -> cellData.getValue().productProperty());
        quantityColumn.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());
        totalColumn.setCellValueFactory(cellData -> cellData.getValue().totalProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        
        // Center align quantity column
        quantityColumn.setStyle("-fx-alignment: CENTER;");
        
        // Custom cell factory for status column with colors
        statusColumn.setCellFactory(column -> new TableCell<TransactionData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String lowerItem = item.toLowerCase();
                    if (lowerItem.equals("completed") || lowerItem.equals("selesai") || lowerItem.equals("sukses")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (lowerItem.equals("pending")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (lowerItem.equals("cancelled") || lowerItem.equals("dibatalkan") || lowerItem.equals("gagal")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #2c3e50;");
                    }
                }
            }
        });
        
        // Set table items
        transactionTable.setItems(filteredTransactions);
        System.out.println("✅ Table columns setup completed");
    }
    
    private void setupFilters() {
        // Status filter options
        statusFilter.setItems(FXCollections.observableArrayList(
            "Semua Status", "Sukses", "Pending", "Gagal", "Dibatalkan", "Selesai"
        ));
        statusFilter.setValue("Semua Status");
        
        // Date filter options
        dateFilter.setItems(FXCollections.observableArrayList(
            "Semua Tanggal", "Hari Ini", "Shift Saya", "Minggu Ini", "Bulan Ini", "Bulan Lalu"
        ));
        dateFilter.setValue("Shift Saya"); // Default to current shift
        
        System.out.println("✅ Filters setup completed");
    }
    
    private void loadAllTransactions() {
        allTransactions.clear();
        
        try {
            System.out.println("📥 Loading transactions from database...");
            
            loadSnackTransactions();
            loadMembershipTransactions();
            loadPenjagaTransactions();
            
            // Sort by date descending (newest first)
            allTransactions.sort((t1, t2) -> {
                try {
                    LocalDateTime dt1 = parseDateTime(t1.getDate());
                    LocalDateTime dt2 = parseDateTime(t2.getDate());
                    return dt2.compareTo(dt1);
                } catch (Exception e) {
                    return 0;
                }
            });
            
            applyFilters();
            
            System.out.println("✅ Total transaksi dimuat: " + allTransactions.size());
            System.out.println("✅ Filtered transaksi: " + filteredTransactions.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            showError("Error Memuat Data", "Gagal memuat data transaksi: " + e.getMessage());
        }
    }
    
    private void loadSnackTransactions() {
        try {
            MongoCollection<Document> snackCollection = database.getCollection("transaksi_snack");
            long count = snackCollection.countDocuments();
            System.out.println("📦 Loading " + count + " snack transactions...");
            
            for (Document doc : snackCollection.find()) {
                try {
                    TransactionData trans = new TransactionData();
                    
                    ObjectId objectId = doc.getObjectId("_id");
                    String fullId = objectId.toHexString();
                    trans.setId(fullId.substring(Math.max(0, fullId.length() - 8)).toUpperCase());
                    trans.setFullId(fullId);
                    trans.setType("snack");
                    
                    String dateStr = extractDate(doc, "transactionDate", "createdAt", "tanggal");
                    trans.setDate(dateStr);
                    
                    String customerName = doc.getString("customerName");
                    if (customerName == null || customerName.isEmpty()) {
                        customerName = "Customer";
                    }
                    trans.setCustomer(customerName);
                    
                    List<Document> items = (List<Document>) doc.get("items");
                    StringBuilder productList = new StringBuilder();
                    int totalItems = 0;
                    
                    if (items != null && !items.isEmpty()) {
                        for (int i = 0; i < Math.min(items.size(), 2); i++) {
                            Document item = items.get(i);
                            String productName = item.getString("productName");
                            if (productName == null) productName = item.getString("name");
                            if (productName == null) productName = "Product";
                            
                            int qty = getIntValue(item, "quantity", 1);
                            totalItems += qty;
                            
                            if (i > 0) productList.append(", ");
                            productList.append(productName).append(" (").append(qty).append(")");
                        }
                        if (items.size() > 2) {
                            productList.append("... (+").append(items.size() - 2).append(" item)");
                        }
                    } else {
                        productList.append("Snack");
                        totalItems = getIntValue(doc, "totalItems", 1);
                    }
                    
                    trans.setProduct(productList.toString());
                    trans.setQuantity(totalItems);
                    
                    int totalPayment = getIntValue(doc, "totalPayment", 0);
                    if (totalPayment == 0) totalPayment = getIntValue(doc, "subtotal", 0);
                    trans.setTotal(currencyFormat.format(totalPayment));
                    trans.setTotalAmount(totalPayment);
                    
                    String status = doc.getString("paymentStatus");
                    if (status == null) status = doc.getString("status");
                    if (status == null) status = "Completed";
                    trans.setStatus(capitalizeFirst(status));
                    
                    allTransactions.add(trans);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error parsing snack transaction: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Loaded " + count + " snack transactions");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading snack transactions: " + e.getMessage());
        }
    }
    
    private void loadMembershipTransactions() {
        try {
            MongoCollection<Document> transCollection = database.getCollection("transactions");
            long count = transCollection.countDocuments();
            System.out.println("👤 Loading " + count + " membership transactions...");
            
            for (Document doc : transCollection.find()) {
                try {
                    TransactionData trans = new TransactionData();
                    
                    ObjectId objectId = doc.getObjectId("_id");
                    String fullId = objectId.toHexString();
                    trans.setId(fullId.substring(Math.max(0, fullId.length() - 8)).toUpperCase());
                    trans.setFullId(fullId);
                    trans.setType("membership");
                    
                    String dateStr = extractDate(doc, "tanggal_transaksi", "createdAt", "created_at");
                    trans.setDate(dateStr);
                    
                    String customerName = doc.getString("nama_member");
                    if (customerName == null || customerName.isEmpty()) {
                        customerName = doc.getString("nama_non_member");
                    }
                    if (customerName == null || customerName.isEmpty()) {
                        customerName = "Member";
                    }
                    trans.setCustomer(customerName);
                    
                    String paket = doc.getString("paket");
                    int durasi = getIntValue(doc, "durasi_hari", 0);
                    String jenisPembayaran = doc.getString("jenis_pembayaran");
                    
                    String product = "Membership";
                    if (paket != null && !paket.isEmpty()) {
                        product += " - " + paket;
                    }
                    if (durasi > 0) {
                        product += " (" + durasi + " hari)";
                    }
                    if (jenisPembayaran != null && !jenisPembayaran.isEmpty() && !jenisPembayaran.equals("member")) {
                        product += " [" + jenisPembayaran + "]";
                    }
                    
                    trans.setProduct(product);
                    trans.setQuantity(1);
                    
                    int totalPayment = getIntValue(doc, "jumlah_dibayar", 0);
                    if (totalPayment == 0) totalPayment = getIntValue(doc, "jumlah", 0);
                    if (totalPayment == 0) totalPayment = getIntValue(doc, "biaya_member", 0);
                    trans.setTotal(currencyFormat.format(totalPayment));
                    trans.setTotalAmount(totalPayment);
                    
                    String status = doc.getString("status");
                    if (status == null) status = "Completed";
                    trans.setStatus(capitalizeFirst(status));
                    
                    allTransactions.add(trans);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error parsing membership transaction: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Loaded " + count + " membership transactions");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading membership transactions: " + e.getMessage());
        }
    }
    
    private void loadPenjagaTransactions() {
        try {
            MongoCollection<Document> penjagaCollection = database.getCollection("penjaga");
            long count = penjagaCollection.countDocuments();
            System.out.println("🚪 Loading " + count + " non-member transactions...");
            
            for (Document doc : penjagaCollection.find()) {
                try {
                    TransactionData trans = new TransactionData();
                    
                    ObjectId objectId = doc.getObjectId("_id");
                    String fullId = objectId.toHexString();
                    trans.setId(fullId.substring(Math.max(0, fullId.length() - 8)).toUpperCase());
                    trans.setFullId(fullId);
                    trans.setType("penjaga");
                    
                    String dateStr = extractDate(doc, "tanggal_masuk", "tanggal_transaksi", "createdAt");
                    trans.setDate(dateStr);
                    
                    String customerName = doc.getString("nama_non_member");
                    if (customerName == null || customerName.isEmpty()) {
                        customerName = "Non Member";
                    }
                    trans.setCustomer(customerName);
                    
                    String jenisPembayaran = doc.getString("jenis_pembayaran");
                    String paket = doc.getString("paket");
                    String tipe = doc.getString("tipe");
                    
                    String product = "Non-Member";
                    if (paket != null && !paket.isEmpty()) {
                        product += " - " + paket;
                    } else if (tipe != null && !tipe.isEmpty()) {
                        product += " - " + tipe;
                    } else if (jenisPembayaran != null && !jenisPembayaran.isEmpty()) {
                        product += " - " + jenisPembayaran.replace("_", " ");
                    }
                    
                    trans.setProduct(product);
                    trans.setQuantity(1);
                    
                    int totalPayment = getIntValue(doc, "jumlah_dibayar", 0);
                    if (totalPayment == 0) totalPayment = getIntValue(doc, "jumlah", 0);
                    trans.setTotal(currencyFormat.format(totalPayment));
                    trans.setTotalAmount(totalPayment);
                    
                    String status = doc.getString("status");
                    if (status == null) status = "selesai";
                    trans.setStatus(capitalizeFirst(status));
                    
                    allTransactions.add(trans);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error parsing penjaga transaction: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Loaded " + count + " non-member transactions");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading penjaga transactions: " + e.getMessage());
        }
    }
    
    private String extractDate(Document doc, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object dateObj = doc.get(fieldName);
            if (dateObj != null) {
                try {
                    if (dateObj instanceof Date) {
                        Date date = (Date) dateObj;
                        LocalDateTime dateTime = date.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
                    } else if (dateObj instanceof String) {
                        String dateStr = (String) dateObj;
                        if (dateStr.contains("T")) {
                            LocalDateTime dateTime = LocalDateTime.parse(dateStr.substring(0, 19));
                            return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
                        }
                        return dateStr;
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error parsing date from field " + fieldName + ": " + e.getMessage());
                }
            }
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }
    
    private LocalDateTime parseDateTime(String dateStr) {
        try {
            if (dateStr.contains(" ")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
            } else {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay();
            }
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    private int getIntValue(Document doc, String key, int defaultValue) {
        Object value = doc.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private void setupEventHandlers() {
        statusFilter.setOnAction(e -> applyFilters());
        dateFilter.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        transactionTable.setRowFactory(tv -> {
            TableRow<TransactionData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showTransactionDetails(row.getItem());
                }
            });
            return row;
        });
    }
    
    private void applyFilters() {
        filteredTransactions.clear();
        
        String statusValue = statusFilter.getValue();
        String dateValue = dateFilter.getValue();
        String searchText = searchField.getText().toLowerCase().trim();
        LocalDate now = LocalDate.now();
        
        for (TransactionData trans : allTransactions) {
            boolean matchStatus = statusValue.equals("Semua Status") || 
                                 trans.getStatus().equalsIgnoreCase(statusValue);
            
            boolean matchDate = dateValue.equals("Semua Tanggal") || 
                               checkDateFilter(trans.getDate(), dateValue, now);
            
            boolean matchSearch = searchText.isEmpty() ||
                                 trans.getCustomer().toLowerCase().contains(searchText) ||
                                 trans.getProduct().toLowerCase().contains(searchText) ||
                                 trans.getId().toLowerCase().contains(searchText);
            
            if (matchStatus && matchDate && matchSearch) {
                filteredTransactions.add(trans);
            }
        }
        
        updateStatistics();
        updateFilterStatus();
        
        System.out.println("🔍 Filter applied. Showing " + filteredTransactions.size() + " of " + allTransactions.size());
    }
    
    private boolean checkDateFilter(String dateStr, String filter, LocalDate now) {
        try {
            LocalDateTime transDateTime = parseDateTime(dateStr);
            LocalDate transDate = transDateTime.toLocalDate();
            
            switch (filter) {
                case "Hari Ini": 
                    return transDate.equals(now);
                case "Shift Saya":
                    return transDateTime.isAfter(shiftStartTime);
                case "Minggu Ini": 
                    return transDate.isAfter(now.minusDays(7)) && !transDate.isAfter(now);
                case "Bulan Ini": 
                    return transDate.getMonth() == now.getMonth() && transDate.getYear() == now.getYear();
                case "Bulan Lalu":
                    LocalDate lastMonth = now.minusMonths(1);
                    return transDate.getMonth() == lastMonth.getMonth() && transDate.getYear() == lastMonth.getYear();
                default: 
                    return true;
            }
        } catch (Exception e) {
            return true;
        }
    }
    
    private void updateStatistics() {
        int totalTrans = filteredTransactions.size();
        totalTransactionsLabel.setText(String.valueOf(totalTrans));
        
        int totalRevenue = filteredTransactions.stream()
            .mapToInt(TransactionData::getTotalAmount)
            .sum();
        
        LocalDate today = LocalDate.now();
        int revenueToday = filteredTransactions.stream()
            .filter(t -> {
                try {
                    LocalDate transDate = parseDateTime(t.getDate()).toLocalDate();
                    return transDate.equals(today);
                } catch (Exception e) {
                    return false;
                }
            })
            .mapToInt(TransactionData::getTotalAmount)
            .sum();
        revenueTodayLabel.setText(currencyFormat.format(revenueToday));
        
        long pendingCount = filteredTransactions.stream()
            .filter(t -> t.getStatus().equalsIgnoreCase("Pending"))
            .count();
        pendingTransactionsLabel.setText(String.valueOf(pendingCount));
        
        int average = totalTrans > 0 ? totalRevenue / totalTrans : 0;
        averageTransactionLabel.setText(currencyFormat.format(average));
    }
    
    private void updateFilterStatus() {
        StringBuilder sb = new StringBuilder("Menampilkan ");
        sb.append(filteredTransactions.size()).append(" dari ")
          .append(allTransactions.size()).append(" transaksi");
        
        if (!statusFilter.getValue().equals("Semua Status") || 
            !dateFilter.getValue().equals("Semua Tanggal") || 
            !searchField.getText().isEmpty()) {
            sb.append(" (terfilter)");
        }
        
        filterStatusLabel.setText(sb.toString());
    }
    
    private void showTransactionDetails(TransactionData transaction) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detail Transaksi");
        alert.setHeaderText("Detail Lengkap Transaksi");
        
        StringBuilder content = new StringBuilder();
        content.append("ID Transaksi: ").append(transaction.getId()).append("\n");
        content.append("Tipe: ").append(transaction.getType().toUpperCase()).append("\n\n");
        content.append("Tanggal: ").append(transaction.getDate()).append("\n");
        content.append("Pelanggan: ").append(transaction.getCustomer()).append("\n");
        content.append("Produk: ").append(transaction.getProduct()).append("\n");
        content.append("Jumlah: ").append(transaction.getQuantity()).append("\n");
        content.append("Total Bayar: ").append(transaction.getTotal()).append("\n");
        content.append("Status: ").append(transaction.getStatus()).append("\n");
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Refreshing data...");
        loadAllTransactions();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Data Diperbarui");
        alert.setHeaderText(null);
        alert.setContentText("Data transaksi berhasil dimuat ulang!\nTotal: " + allTransactions.size() + " transaksi");
        alert.showAndWait();
    }
    
    /**
     * Export shift transactions to Excel
     */
    @FXML
    private void handleExportExcel() {
        // Filter only shift transactions
        List<TransactionData> shiftTransactions = new ArrayList<>();
        for (TransactionData trans : allTransactions) {
            LocalDateTime transDateTime = parseDateTime(trans.getDate());
            if (transDateTime.isAfter(shiftStartTime) || transDateTime.isEqual(shiftStartTime)) {
                shiftTransactions.add(trans);
            }
        }
        
        if (shiftTransactions.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Tidak Ada Data");
            alert.setHeaderText(null);
            alert.setContentText("Tidak ada transaksi selama shift ini untuk di-export.");
            alert.showAndWait();
            return;
        }
        
        // File chooser for save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Rekap Excel");
        fileChooser.setInitialFileName("Rekap_Shift_" + currentGuardName + "_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        
        Stage stage = (Stage) transactionTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                exportToExcel(shiftTransactions, file);
                
                // Also save to MongoDB
                saveShiftRecap(shiftTransactions);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Berhasil");
                alert.setHeaderText(null);
                alert.setContentText("Rekap shift berhasil di-export ke:\n" + file.getAbsolutePath() + 
                    "\n\nTotal Transaksi: " + shiftTransactions.size());
                alert.showAndWait();
                
            } catch (Exception e) {
                System.err.println("❌ Error exporting to Excel: " + e.getMessage());
                e.printStackTrace();
                showError("Error Export", "Gagal meng-export ke Excel: " + e.getMessage());
            }
        }
    }
    
    /**
     * Export transactions to Excel file
     */
    private void exportToExcel(List<TransactionData> transactions, File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Rekap Shift");
        
        // Create fonts
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        
        // Create styles
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        
        CellStyle tableHeaderStyle = workbook.createCellStyle();
        tableHeaderStyle.setFont(titleFont);
        tableHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
        tableHeaderStyle.setBorderTop(BorderStyle.THIN);
        tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
        tableHeaderStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.cloneStyleFrom(cellStyle);
        currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REKAP SHIFT TRANSAKSI");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        
        rowNum++;
        
        // Guard info
        Row guardRow = sheet.createRow(rowNum++);
        guardRow.createCell(0).setCellValue("Penjaga:");
        guardRow.createCell(1).setCellValue(currentGuardName);
        
        Row startRow = sheet.createRow(rowNum++);
        startRow.createCell(0).setCellValue("Mulai Shift:");
        startRow.createCell(1).setCellValue(shiftStartTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        
        Row endRow = sheet.createRow(rowNum++);
        endRow.createCell(0).setCellValue("Akhir Shift:");
        endRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        
        rowNum++;
        
        // Statistics
        int totalRevenue = transactions.stream().mapToInt(TransactionData::getTotalAmount).sum();
        long snackCount = transactions.stream().filter(t -> t.getType().equals("snack")).count();
        long membershipCount = transactions.stream().filter(t -> t.getType().equals("membership")).count();
        long nonMemberCount = transactions.stream().filter(t -> t.getType().equals("penjaga")).count();
        
        int snackRevenue = transactions.stream().filter(t -> t.getType().equals("snack"))
            .mapToInt(TransactionData::getTotalAmount).sum();
        int membershipRevenue = transactions.stream().filter(t -> t.getType().equals("membership"))
            .mapToInt(TransactionData::getTotalAmount).sum();
        int nonMemberRevenue = transactions.stream().filter(t -> t.getType().equals("penjaga"))
            .mapToInt(TransactionData::getTotalAmount).sum();
        
        Row statsHeaderRow = sheet.createRow(rowNum++);
        Cell statsHeaderCell = statsHeaderRow.createCell(0);
        statsHeaderCell.setCellValue("RINGKASAN");
        statsHeaderCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));
        
        Row totalTransRow = sheet.createRow(rowNum++);
        totalTransRow.createCell(0).setCellValue("Total Transaksi:");
        totalTransRow.createCell(1).setCellValue(transactions.size());
        
        Row totalRevRow = sheet.createRow(rowNum++);
        totalRevRow.createCell(0).setCellValue("Total Pendapatan:");
        Cell revCell = totalRevRow.createCell(1);
        revCell.setCellValue(totalRevenue);
        revCell.setCellStyle(currencyStyle);
        
        rowNum++;
        
        // Category breakdown
        Row categoryHeaderRow = sheet.createRow(rowNum++);
        categoryHeaderRow.createCell(0).setCellValue("Kategori");
        categoryHeaderRow.createCell(1).setCellValue("Jumlah");
        categoryHeaderRow.createCell(2).setCellValue("Pendapatan");
        for (int i = 0; i < 3; i++) {
            categoryHeaderRow.getCell(i).setCellStyle(tableHeaderStyle);
        }
        
        Row snackRow = sheet.createRow(rowNum++);
        snackRow.createCell(0).setCellValue("Snack");
        snackRow.createCell(1).setCellValue(snackCount);
        Cell snackRevCell = snackRow.createCell(2);
        snackRevCell.setCellValue(snackRevenue);
        snackRevCell.setCellStyle(currencyStyle);
        
        Row memberRow = sheet.createRow(rowNum++);
        memberRow.createCell(0).setCellValue("Membership");
        memberRow.createCell(1).setCellValue(membershipCount);
        Cell memberRevCell = memberRow.createCell(2);
        memberRevCell.setCellValue(membershipRevenue);
        memberRevCell.setCellStyle(currencyStyle);
        
        Row nonMemberRow = sheet.createRow(rowNum++);
        nonMemberRow.createCell(0).setCellValue("Non-Member");
        nonMemberRow.createCell(1).setCellValue(nonMemberCount);
        Cell nonMemberRevCell = nonMemberRow.createCell(2);
        nonMemberRevCell.setCellValue(nonMemberRevenue);
        nonMemberRevCell.setCellStyle(currencyStyle);
        
        rowNum += 2;
        
        // Transaction table header
        Row tableHeaderRow = sheet.createRow(rowNum++);
        Cell thCell1 = tableHeaderRow.createCell(0);
        thCell1.setCellValue("DETAIL TRANSAKSI");
        thCell1.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 6));
        
        Row columnHeaderRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Tanggal", "Pelanggan", "Produk", "Jumlah", "Total", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = columnHeaderRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(tableHeaderStyle);
        }
        
        // Transaction data
        for (TransactionData trans : transactions) {
            Row row = sheet.createRow(rowNum++);
            
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(trans.getId());
            cell0.setCellStyle(cellStyle);
            
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(trans.getDate());
            cell1.setCellStyle(cellStyle);
            
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(trans.getCustomer());
            cell2.setCellStyle(cellStyle);
            
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(trans.getProduct());
            cell3.setCellStyle(cellStyle);
            
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(trans.getQuantity());
            cell4.setCellStyle(cellStyle);
            
            Cell cell5 = row.createCell(5);
            cell5.setCellValue(trans.getTotalAmount());
            cell5.setCellStyle(currencyStyle);
            
            Cell cell6 = row.createCell(6);
            cell6.setCellValue(trans.getStatus());
            cell6.setCellStyle(cellStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }
        
        workbook.close();
        System.out.println("✅ Excel file created successfully: " + file.getAbsolutePath());
    }
    
    /**
     * Save shift recap to MongoDB
     */
    private void saveShiftRecap(List<TransactionData> transactions) {
        try {
            MongoCollection<Document> rekapCollection = database.getCollection("rekap_shift");
            
            // Calculate statistics
            int totalTransactions = transactions.size();
            int totalRevenue = transactions.stream()
                .mapToInt(TransactionData::getTotalAmount)
                .sum();
            
            // Count by type
            long snackCount = transactions.stream()
                .filter(t -> t.getType().equals("snack"))
                .count();
            long membershipCount = transactions.stream()
                .filter(t -> t.getType().equals("membership"))
                .count();
            long nonMemberCount = transactions.stream()
                .filter(t -> t.getType().equals("penjaga"))
                .count();
            
            // Revenue by type
            int snackRevenue = transactions.stream()
                .filter(t -> t.getType().equals("snack"))
                .mapToInt(TransactionData::getTotalAmount)
                .sum();
            int membershipRevenue = transactions.stream()
                .filter(t -> t.getType().equals("membership"))
                .mapToInt(TransactionData::getTotalAmount)
                .sum();
            int nonMemberRevenue = transactions.stream()
                .filter(t -> t.getType().equals("penjaga"))
                .mapToInt(TransactionData::getTotalAmount)
                .sum();
            
            // Create transaction list
            List<Document> transactionList = new ArrayList<>();
            for (TransactionData trans : transactions) {
                Document transDoc = new Document()
                    .append("id", trans.getFullId())
                    .append("type", trans.getType())
                    .append("date", trans.getDate())
                    .append("customer", trans.getCustomer())
                    .append("product", trans.getProduct())
                    .append("quantity", trans.getQuantity())
                    .append("total", trans.getTotalAmount())
                    .append("status", trans.getStatus());
                transactionList.add(transDoc);
            }
            
            // Create recap document
            Document rekapDoc = new Document()
                .append("guard_name", currentGuardName)
                .append("guard_id", currentGuardId)
                .append("shift_start", Date.from(shiftStartTime.atZone(ZoneId.systemDefault()).toInstant()))
                .append("shift_end", new Date())
                .append("total_transactions", totalTransactions)
                .append("total_revenue", totalRevenue)
                .append("statistics", new Document()
                    .append("snack", new Document()
                        .append("count", snackCount)
                        .append("revenue", snackRevenue))
                    .append("membership", new Document()
                        .append("count", membershipCount)
                        .append("revenue", membershipRevenue))
                    .append("non_member", new Document()
                        .append("count", nonMemberCount)
                        .append("revenue", nonMemberRevenue)))
                .append("transactions", transactionList)
                .append("created_at", new Date());
            
            // Insert to database
            rekapCollection.insertOne(rekapDoc);
            
            // Update active shift status
            updateShiftStatus();
            
            System.out.println("✅ Shift recap saved to MongoDB successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error saving shift recap: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update shift status when recap is saved
     */
    private void updateShiftStatus() {
        try {
            MongoCollection<Document> shiftCollection = database.getCollection("active_shifts");
            
            Document filter = new Document("guard_name", currentGuardName)
                .append("shift_end", null);
            
            Document update = new Document("$set", new Document()
                .append("shift_end", new Date())
                .append("status", "completed"));
            
            shiftCollection.updateOne(filter, update);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating shift status: " + e.getMessage());
        }
    }
    
    /**
     * View shift history from database
     */
    @FXML
    private void handleViewShiftHistory() {
        try {
            MongoCollection<Document> rekapCollection = database.getCollection("rekap_shift");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Riwayat Rekap Shift");
            alert.setHeaderText("10 Rekap Shift Terakhir");
            
            StringBuilder content = new StringBuilder();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            int count = 0;
            for (Document doc : rekapCollection.find().sort(new Document("created_at", -1)).limit(10)) {
                count++;
                
                String guardName = doc.getString("guard_name");
                Date shiftStart = doc.getDate("shift_start");
                Date shiftEnd = doc.getDate("shift_end");
                int totalTrans = doc.getInteger("total_transactions", 0);
                int totalRev = doc.getInteger("total_revenue", 0);
                
                LocalDateTime startTime = shiftStart.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                LocalDateTime endTime = shiftEnd.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                
                content.append(count).append(". ").append(guardName).append("\n");
                content.append("   ").append(startTime.format(formatter))
                       .append(" - ").append(endTime.format(formatter)).append("\n");
                content.append("   Transaksi: ").append(totalTrans)
                       .append(" | Pendapatan: ").append(currencyFormat.format(totalRev)).append("\n\n");
            }
            
            if (count == 0) {
                content.append("Belum ada riwayat rekap shift.");
            }
            
            alert.setContentText(content.toString());
            alert.showAndWait();
            
        } catch (Exception e) {
            System.err.println("❌ Error loading shift history: " + e.getMessage());
            showError("Error", "Gagal memuat riwayat shift: " + e.getMessage());
        }
    }
    
    /**
     * Handle back button - return to main menu
     */
    @FXML
    private void handleBack() {
        try {
            // Stop clock timeline
            if (clockTimeline != null) {
                clockTimeline.stop();
            }
            
            // Load main menu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("menu_transaksi.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) transactionTable.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
            System.out.println("✅ Returned to main menu");
            
        } catch (IOException e) {
            System.err.println("❌ Error returning to main menu: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "Gagal kembali ke menu utama: " + e.getMessage());
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }
}

// ===== TRANSACTION DATA MODEL CLASS =====
class TransactionData {
    private final SimpleStringProperty id;
    private String fullId;
    private String type;
    private final SimpleStringProperty date;
    private final SimpleStringProperty customer;
    private final SimpleStringProperty product;
    private final SimpleIntegerProperty quantity;
    private final SimpleStringProperty total;
    private int totalAmount;
    private final SimpleStringProperty status;
    
    public TransactionData() {
        this.id = new SimpleStringProperty("");
        this.date = new SimpleStringProperty("");
        this.customer = new SimpleStringProperty("");
        this.product = new SimpleStringProperty("");
        this.quantity = new SimpleIntegerProperty(0);
        this.total = new SimpleStringProperty("");
        this.status = new SimpleStringProperty("");
    }
    
    // Property methods for JavaFX binding
    public SimpleStringProperty idProperty() { return id; }
    public SimpleStringProperty dateProperty() { return date; }
    public SimpleStringProperty customerProperty() { return customer; }
    public SimpleStringProperty productProperty() { return product; }
    public SimpleIntegerProperty quantityProperty() { return quantity; }
    public SimpleStringProperty totalProperty() { return total; }
    public SimpleStringProperty statusProperty() { return status; }
    
    // Getters
    public String getId() { return id.get(); }
    public String getFullId() { return fullId; }
    public String getType() { return type; }
    public String getDate() { return date.get(); }
    public String getCustomer() { return customer.get(); }
    public String getProduct() { return product.get(); }
    public int getQuantity() { return quantity.get(); }
    public String getTotal() { return total.get(); }
    public int getTotalAmount() { return totalAmount; }
    public String getStatus() { return status.get(); }
    
    // Setters
    public void setId(String id) { this.id.set(id); }
    public void setFullId(String fullId) { this.fullId = fullId; }
    public void setType(String type) { this.type = type; }
    public void setDate(String date) { this.date.set(date); }
    public void setCustomer(String customer) { this.customer.set(customer); }
    public void setProduct(String product) { this.product.set(product); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    public void setTotal(String total) { this.total.set(total); }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status) { this.status.set(status); }
}