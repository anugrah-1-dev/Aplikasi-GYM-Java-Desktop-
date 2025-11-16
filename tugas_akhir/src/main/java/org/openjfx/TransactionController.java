package org.openjfx;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionController {

    @FXML private Label guardNameLabel;
    @FXML private Label timeLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label revenueTodayLabel;
    @FXML private Label pendingTransactionsLabel;
    @FXML private Label averageTransactionLabel;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> dateFilter;
    @FXML private TextField searchField;
    @FXML private Label filterStatusLabel;
    @FXML private TableView<TransactionData> transactionTable;
    @FXML private TableColumn<TransactionData, String> idColumn;
    @FXML private TableColumn<TransactionData, String> dateColumn;
    @FXML private TableColumn<TransactionData, String> customerColumn;
    @FXML private TableColumn<TransactionData, String> productColumn;
    @FXML private TableColumn<TransactionData, String> quantityColumn;
    @FXML private TableColumn<TransactionData, String> totalColumn;
    @FXML private TableColumn<TransactionData, String> petugasColumn;
    @FXML private TableColumn<TransactionData, String> statusColumn;

    private MongoClient mongoClient;
    private MongoDatabase database;
    private ObservableList<TransactionData> allTransactions;
    private Timer timeTimer;
    private String currentGuardName = "Admin";
    private final NumberFormat currencyFormat = new DecimalFormat("#,###");

    @FXML
    public void initialize() {
        System.out.println("🔄 TransactionController initialized");
        
        // Connect to MongoDB
        connectToDatabase();
        
        // Setup table columns
        setupTableColumns();
        
        // Setup filters
        setupFilters();
        
        // Load initial data
        loadAllTransactions();
        
        // Update statistics
        updateStatistics();
        
        // Setup time updater
        startTimeUpdater();
        
        // Setup guard name
        guardNameLabel.setText("Penjaga: " + currentGuardName);
        
        // Setup search listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTransactions());
        
        // Setup double-click to view details
        setupTableDoubleClick();
        
        System.out.println("✅ Controller initialized successfully");
        System.out.println("📊 Current guard: " + currentGuardName);
        System.out.println("📋 Total transaksi: " + (allTransactions != null ? allTransactions.size() : 0));
    }

    private void connectToDatabase() {
        try {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("gym");
            System.out.println("✅ Connected to MongoDB");
        } catch (Exception e) {
            System.err.println("❌ Database connection error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> data.getValue().idProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        customerColumn.setCellValueFactory(data -> data.getValue().customerProperty());
        productColumn.setCellValueFactory(data -> data.getValue().productProperty());
        quantityColumn.setCellValueFactory(data -> data.getValue().quantityProperty());
        totalColumn.setCellValueFactory(data -> data.getValue().totalProperty());
        petugasColumn.setCellValueFactory(data -> data.getValue().petugasProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        
        // Style status column with colors
        statusColumn.setCellFactory(column -> new TableCell<TransactionData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Completed") || item.equalsIgnoreCase("selesai")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("Pending")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        // Status filter
        statusFilter.setItems(FXCollections.observableArrayList(
            "Semua Status", "Completed", "Pending", "Cancelled"
        ));
        statusFilter.setValue("Semua Status");
        statusFilter.setOnAction(e -> filterTransactions());
        
        // Date filter
        dateFilter.setItems(FXCollections.observableArrayList(
            "Semua Tanggal", "Hari Ini", "Minggu Ini", "Bulan Ini"
        ));
        dateFilter.setValue("Semua Tanggal");
        dateFilter.setOnAction(e -> filterTransactions());
    }

    private void setupTableDoubleClick() {
        transactionTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TransactionData selected = transactionTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showTransactionDetails(selected);
                }
            }
        });
    }

    private void loadAllTransactions() {
        System.out.println("📥 Loading transactions...");
        allTransactions = FXCollections.observableArrayList();
        
        try {
            // Load membership transactions
            MongoCollection<Document> memberCollection = database.getCollection("transactions");
            List<Document> memberDocs = memberCollection.find().into(new ArrayList<>());
            
            for (Document doc : memberDocs) {
                try {
                    String id = doc.getString("non_member_id");
                    if (id == null) id = "N/A";
                    
                    String date = doc.getString("tanggal_transaksi");
                    if (date == null) date = "N/A";
                    
                    String customer = doc.getString("nama_non_member");
                    if (customer == null) customer = "N/A";
                    
                    String paket = doc.getString("paket");
                    String jenis = doc.getString("jenis_pembayaran");
                    String product = (paket != null ? paket : "N/A") + " - " + (jenis != null ? jenis : "N/A");
                    
                    int quantity = 1;
                    
                    // Handle both Integer and Double for total
                    int total = 0;
                    Object totalObj = doc.get("jumlah_dibayar");
                    if (totalObj instanceof Integer) {
                        total = (Integer) totalObj;
                    } else if (totalObj instanceof Double) {
                        total = ((Double) totalObj).intValue();
                    }
                    
                    String status = doc.getString("status");
                    if (status == null) status = "N/A";
                    
                    String petugas = doc.getString("petugas_nama");
                    if (petugas == null) petugas = "N/A";
                    
                    String type = "membership";
                    
                    allTransactions.add(new TransactionData(
                        id, date, customer, product, quantity, total, petugas, status, type, doc
                    ));
                } catch (Exception e) {
                    System.err.println("❌ Error parsing membership transaction: " + e.getMessage());
                }
            }
            
            // Load snack transactions
            MongoCollection<Document> snackCollection = database.getCollection("transaksi_snack");
            List<Document> snackDocs = snackCollection.find().into(new ArrayList<>());
            
            System.out.println("📦 Found " + snackDocs.size() + " snack documents");
            
            for (Document doc : snackDocs) {
                try {
                    System.out.println("\n=== Processing Snack Document ===");
                    System.out.println("Raw Document: " + doc.toJson());
                    
                    String id = doc.getString("transactionId");
                    if (id == null) id = "N/A";
                    System.out.println("ID: " + id);
                    
                    // Handle Date object for transactionDate
                    String date = "N/A";
                    Object dateObj = doc.get("transactionDate");
                    if (dateObj instanceof String) {
                        date = (String) dateObj;
                    } else if (dateObj instanceof Date) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        date = sdf.format((Date) dateObj);
                    }
                    
                    String customer = doc.getString("customerName");
                    if (customer == null) customer = "N/A";
                    System.out.println("Customer: " + customer);
                    
                    // Get product details - handle both List and Document types
                    StringBuilder products = new StringBuilder();
                    int totalQty = 0;
                    
                    Object itemsObj = doc.get("items");
                    System.out.println("Items Type: " + (itemsObj != null ? itemsObj.getClass().getName() : "null"));
                    System.out.println("Items Content: " + itemsObj);
                    if (itemsObj instanceof List) {
                        List<?> itemsList = (List<?>) itemsObj;
                        boolean firstItem = true;
                        for (Object itemObj : itemsList) {
                            if (itemObj instanceof Document) {
                                Document item = (Document) itemObj;
                                Object qtyObj = item.get("quantity");
                                int qty = 0;
                                if (qtyObj instanceof Integer) {
                                    qty = (Integer) qtyObj;
                                } else if (qtyObj instanceof Double) {
                                    qty = ((Double) qtyObj).intValue();
                                }
                                
                                String prodName = item.getString("productName");
                                if (prodName != null) {
                                    if (!firstItem) products.append(", ");
                                    products.append(prodName);
                                    if (qty > 1) products.append(" x").append(qty);
                                    totalQty += qty;
                                    firstItem = false;
                                }
                            }
                        }
                    } else if (itemsObj instanceof Document) {
                        Document items = (Document) itemsObj;
                        int count = 0;
                        for (String key : items.keySet()) {
                            Object itemValue = items.get(key);
                            if (itemValue instanceof Document) {
                                Document item = (Document) itemValue;
                                Object qtyObj = item.get("quantity");
                                int qty = 0;
                                if (qtyObj instanceof Integer) {
                                    qty = (Integer) qtyObj;
                                } else if (qtyObj instanceof Double) {
                                    qty = ((Double) qtyObj).intValue();
                                }
                                
                                String prodName = item.getString("productName");
                                if (prodName != null) {
                                    if (count > 0) products.append(", ");
                                    products.append(prodName);
                                    if (qty > 1) products.append(" x").append(qty);
                                    totalQty += qty;
                                    count++;
                                }
                            }
                        }
                    }
                    
                    // Ensure we have valid product data
                    if (products.length() == 0) {
                        products.append("N/A");
                    }
                    if (totalQty == 0) {
                        totalQty = 1; // Default to 1 if no quantity found
                    }
                    
                    // Handle both Integer and Double for total
                    int total = 0;
                    Object totalObj = doc.get("totalPayment");
                    if (totalObj instanceof Integer) {
                        total = (Integer) totalObj;
                    } else if (totalObj instanceof Double) {
                        total = ((Double) totalObj).intValue();
                    }
                    
                    String status = doc.getString("status");
                    if (status == null) status = "N/A";
                    
                    String petugas = doc.getString("petugas_username");
                    if (petugas == null) petugas = "Kasir";
                    
                    String type = "snack";
                    
                    allTransactions.add(new TransactionData(
                        id, date, customer, products.toString(), totalQty, total, petugas, status, type, doc
                    ));
                    
                    System.out.println("✅ Snack transaction added: " + id + " - " + products.toString());
                    
                } catch (Exception e) {
                    System.err.println("❌ Error parsing snack transaction: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Sort by date (newest first)
            allTransactions.sort((t1, t2) -> {
                try {
                    return t2.getDate().compareTo(t1.getDate());
                } catch (Exception e) {
                    return 0;
                }
            });
            
            transactionTable.setItems(allTransactions);
            System.out.println("✅ Loaded " + allTransactions.size() + " transactions");
            
        } catch (Exception e) {
            System.err.println("❌ Load transactions error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Load Error", 
                     "Failed to load transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterTransactions() {
        String statusValue = statusFilter.getValue();
        String dateValue = dateFilter.getValue();
        String searchText = searchField.getText().toLowerCase();
        
        List<TransactionData> filtered = allTransactions.stream()
            .filter(t -> {
                // Status filter
                if (!statusValue.equals("Semua Status")) {
                    String status = t.getStatus();
                    if (status == null || !status.equalsIgnoreCase(statusValue)) return false;
                }
                
                // Date filter
                if (!dateValue.equals("Semua Tanggal")) {
                    if (!matchesDateFilter(t.getDate(), dateValue)) return false;
                }
                
                // Search filter
                if (!searchText.isEmpty()) {
                    String id = t.getId() != null ? t.getId().toLowerCase() : "";
                    String customer = t.getCustomer() != null ? t.getCustomer().toLowerCase() : "";
                    String product = t.getProduct() != null ? t.getProduct().toLowerCase() : "";
                    String petugas = t.getPetugas() != null ? t.getPetugas().toLowerCase() : "";
                    
                    return id.contains(searchText) || 
                           customer.contains(searchText) || 
                           product.contains(searchText) ||
                           petugas.contains(searchText);
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        transactionTable.setItems(FXCollections.observableArrayList(filtered));
        filterStatusLabel.setText("Menampilkan " + filtered.size() + " dari " + 
                                  allTransactions.size() + " transaksi");
        
        updateStatistics();
    }

    private boolean matchesDateFilter(String dateStr, String filter) {
        try {
            if (dateStr == null || dateStr.equals("N/A")) return false;
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date transDate = sdf.parse(dateStr.substring(0, 10));
            Calendar transCal = Calendar.getInstance();
            transCal.setTime(transDate);
            
            Calendar now = Calendar.getInstance();
            
            switch (filter) {
                case "Hari Ini":
                    return transCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                           transCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
                
                case "Minggu Ini":
                    return transCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                           transCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR);
                
                case "Bulan Ini":
                    return transCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                           transCal.get(Calendar.MONTH) == now.get(Calendar.MONTH);
                
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStatistics() {
        ObservableList<TransactionData> currentList = transactionTable.getItems();
        
        // Total transactions
        totalTransactionsLabel.setText(String.valueOf(currentList.size()));
        
        // Revenue today
        Calendar today = Calendar.getInstance();
        int revenueToday = currentList.stream()
            .filter(t -> {
                try {
                    String dateStr = t.getDate();
                    if (dateStr == null || dateStr.equals("N/A")) return false;
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date transDate = sdf.parse(dateStr.substring(0, 10));
                    Calendar transCal = Calendar.getInstance();
                    transCal.setTime(transDate);
                    return transCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                           transCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
                } catch (Exception e) {
                    return false;
                }
            })
            .mapToInt(TransactionData::getTotalInt)
            .sum();
        revenueTodayLabel.setText("Rp " + currencyFormat.format(revenueToday));
        
        // Pending transactions - handle null status
        long pending = currentList.stream()
            .filter(t -> {
                String status = t.getStatus();
                return status != null && status.equalsIgnoreCase("Pending");
            })
            .count();
        pendingTransactionsLabel.setText(String.valueOf(pending));
        
        // Average transaction
        double average = currentList.isEmpty() ? 0 : 
            currentList.stream()
                .mapToInt(TransactionData::getTotalInt)
                .average()
                .orElse(0);
        averageTransactionLabel.setText("Rp " + currencyFormat.format((int) average));
    }

    private void showTransactionDetails(TransactionData transaction) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detail Transaksi");
        alert.setHeaderText("Detail Transaksi: " + transaction.getId());
        
        StringBuilder details = new StringBuilder();
        Document doc = transaction.getOriginalDocument();
        
        if (transaction.getType().equals("membership")) {
            details.append("Tipe: Membership\n");
            details.append("ID Non-Member: ").append(doc.getString("non_member_id")).append("\n");
            details.append("Nama: ").append(doc.getString("nama_non_member")).append("\n");
            details.append("Paket: ").append(doc.getString("paket")).append("\n");
            details.append("Jenis: ").append(doc.getString("jenis_pembayaran")).append("\n");
            details.append("Tipe: ").append(doc.getString("tipe")).append("\n");
            
            Object jumlahObj = doc.get("jumlah");
            int jumlah = jumlahObj instanceof Double ? ((Double) jumlahObj).intValue() : doc.getInteger("jumlah", 0);
            details.append("Jumlah: Rp ").append(currencyFormat.format(jumlah)).append("\n");
            
            Object dibayarObj = doc.get("jumlah_dibayar");
            int dibayar = dibayarObj instanceof Double ? ((Double) dibayarObj).intValue() : doc.getInteger("jumlah_dibayar", 0);
            details.append("Dibayar: Rp ").append(currencyFormat.format(dibayar)).append("\n");
            
            Object kembalianObj = doc.get("kembalian");
            int kembalian = kembalianObj instanceof Double ? ((Double) kembalianObj).intValue() : doc.getInteger("kembalian", 0);
            details.append("Kembalian: Rp ").append(currencyFormat.format(kembalian)).append("\n");
            
            details.append("Status: ").append(doc.getString("status")).append("\n");
            details.append("Petugas: ").append(doc.getString("petugas_nama")).append("\n");
        } else {
            details.append("Tipe: Snack/Minuman\n");
            details.append("ID Transaksi: ").append(doc.getString("transactionId")).append("\n");
            details.append("Pelanggan: ").append(doc.getString("customerName")).append("\n");
            
            Object itemsObj = doc.get("items");
            details.append("\nProduk:\n");
            if (itemsObj instanceof List) {
                List<?> itemsList = (List<?>) itemsObj;
                for (Object itemObj : itemsList) {
                    if (itemObj instanceof Document) {
                        Document item = (Document) itemObj;
                        Object qtyObj = item.get("quantity");
                        int qty = 0;
                        if (qtyObj instanceof Integer) {
                            qty = (Integer) qtyObj;
                        } else if (qtyObj instanceof Double) {
                            qty = ((Double) qtyObj).intValue();
                        }
                        details.append("  - ").append(item.getString("productName"))
                               .append(" x").append(qty)
                               .append("\n");
                    }
                }
            } else if (itemsObj instanceof Document) {
                Document items = (Document) itemsObj;
                for (String key : items.keySet()) {
                    Object itemValue = items.get(key);
                    if (itemValue instanceof Integer) {
                        int qty = (Integer) itemValue;
                        details.append("  - ").append(key)
                               .append(" x").append(qty)
                               .append("\n");
                    } else if (itemValue instanceof Double) {
                        int qty = ((Double) itemValue).intValue();
                        details.append("  - ").append(key)
                               .append(" x").append(qty)
                               .append("\n");
                    } else if (itemValue instanceof Document) {
                        Document item = (Document) itemValue;
                        Object qtyObj = item.get("quantity");
                        int qty = 0;
                        if (qtyObj instanceof Integer) {
                            qty = (Integer) qtyObj;
                        } else if (qtyObj instanceof Double) {
                            qty = ((Double) qtyObj).intValue();
                        }
                        String prodName = item.getString("productName");
                        if (prodName != null) {
                            details.append("  - ").append(prodName)
                                   .append(" x").append(qty)
                                   .append("\n");
                        }
                    }
                }
            }
            
            Object subtotalObj = doc.get("subtotal");
            int subtotal = subtotalObj instanceof Double ? ((Double) subtotalObj).intValue() : doc.getInteger("subtotal", 0);
            details.append("\nSubtotal: Rp ").append(currencyFormat.format(subtotal)).append("\n");
            
            Object taxObj = doc.get("tax");
            int tax = taxObj instanceof Double ? ((Double) taxObj).intValue() : doc.getInteger("tax", 0);
            details.append("Pajak: Rp ").append(currencyFormat.format(tax)).append("\n");
            
            Object totalObj = doc.get("totalPayment");
            int total = totalObj instanceof Double ? ((Double) totalObj).intValue() : doc.getInteger("totalPayment", 0);
            details.append("Total: Rp ").append(currencyFormat.format(total)).append("\n");
            
            Object cashObj = doc.get("cashGiven");
            int cash = cashObj instanceof Double ? ((Double) cashObj).intValue() : doc.getInteger("cashGiven", 0);
            details.append("Uang Diterima: Rp ").append(currencyFormat.format(cash)).append("\n");
            
            Object changeObj = doc.get("change");
            int change = changeObj instanceof Double ? ((Double) changeObj).intValue() : doc.getInteger("change", 0);
            details.append("Kembalian: Rp ").append(currencyFormat.format(change)).append("\n");
            
            details.append("Status: ").append(doc.getString("status")).append("\n");
        }
        
        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Refreshing data...");
        loadAllTransactions();
        updateStatistics();
        filterStatusLabel.setText("Data berhasil di-refresh!");
    }

    @FXML
    private void handleViewShiftHistory() {
        showAlert(Alert.AlertType.INFORMATION, "Riwayat Shift", 
                 "Fitur riwayat shift akan segera tersedia!");
    }

    @FXML
    private void handleExportExcel() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export to Excel");
            fileChooser.setInitialFileName("Transaksi_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );
            
            File file = fileChooser.showSaveDialog(transactionTable.getScene().getWindow());
            
            if (file != null) {
                exportToExcel(file);
                showAlert(Alert.AlertType.INFORMATION, "Export Berhasil", 
                         "Data berhasil diekspor ke:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Error", 
                     "Failed to export: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportToExcel(File file) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Transaksi");
        
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Tanggal", "Pelanggan", "Produk", "Jumlah", "Total", "Petugas", "Status", "Tipe"};
        
        for (int i = 0; i < columns.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        ObservableList<TransactionData> data = transactionTable.getItems();
        int rowNum = 1;
        
        for (TransactionData transaction : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(transaction.getId());
            row.createCell(1).setCellValue(transaction.getDate());
            row.createCell(2).setCellValue(transaction.getCustomer());
            row.createCell(3).setCellValue(transaction.getProduct());
            row.createCell(4).setCellValue(transaction.getQuantity());
            row.createCell(5).setCellValue(transaction.getTotal());
            row.createCell(6).setCellValue(transaction.getPetugas());
            row.createCell(7).setCellValue(transaction.getStatus());
            row.createCell(8).setCellValue(transaction.getType());
        }
        
        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Write to file
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        
        workbook.close();
    }

    @FXML
    private void handleBack() {
        try {
            if (timeTimer != null) {
                timeTimer.cancel();
            }
            if (mongoClient != null) {
                mongoClient.close();
            }
            
            // Get current stage
            javafx.stage.Stage stage = (javafx.stage.Stage) transactionTable.getScene().getWindow();
            
            // Try to load MenuTransaksi
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_transaksi/menu_transaksi.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(root);
                stage.setScene(scene);
                stage.show();
                System.out.println("✅ Successfully returned to MenuTransaksi");
            } catch (Exception loadError) {
                System.err.println("❌ Failed to load MenuTransaksi: " + loadError.getMessage());
                // If loading fails, just close the window
                stage.hide();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error going back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startTimeUpdater() {
        timeTimer = new Timer(true);
        timeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    timeLabel.setText(LocalDateTime.now().format(formatter));
                });
            }
        }, 0, 1000);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Inner class for transaction data
    public static class TransactionData {
        private final SimpleStringProperty id;
        private final SimpleStringProperty date;
        private final SimpleStringProperty customer;
        private final SimpleStringProperty product;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty total;
        private final SimpleStringProperty petugas;
        private final SimpleStringProperty status;
        private final String type;
        private final int totalInt;
        private final int quantityInt;
        private final Document originalDocument;
        
        public TransactionData(String id, String date, String customer, String product, 
                             int quantity, int total, String petugas, String status, String type, Document doc) {
            this.id = new SimpleStringProperty(id != null ? id : "N/A");
            this.date = new SimpleStringProperty(date != null ? date : "N/A");
            this.customer = new SimpleStringProperty(customer != null ? customer : "N/A");
            this.product = new SimpleStringProperty(product != null ? product : "N/A");
            this.quantity = new SimpleStringProperty(String.valueOf(quantity));
            this.total = new SimpleStringProperty("Rp " + new DecimalFormat("#,###").format(total));
            this.petugas = new SimpleStringProperty(petugas != null ? petugas : "N/A");
            this.status = new SimpleStringProperty(status != null ? status : "N/A");
            this.type = type;
            this.totalInt = total;
            this.quantityInt = quantity;
            this.originalDocument = doc;
        }
        
        public SimpleStringProperty idProperty() { return id; }
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty customerProperty() { return customer; }
        public SimpleStringProperty productProperty() { return product; }
        public SimpleStringProperty quantityProperty() { return quantity; }
        public SimpleStringProperty totalProperty() { return total; }
        public SimpleStringProperty petugasProperty() { return petugas; }
        public SimpleStringProperty statusProperty() { return status; }
        
        public String getId() { return id.get(); }
        public String getDate() { return date.get(); }
        public String getCustomer() { return customer.get(); }
        public String getProduct() { return product.get(); }
        public String getQuantity() { return String.valueOf(quantityInt); }
        public String getTotal() { return total.get(); }
        public String getPetugas() { return petugas.get(); }
        public String getStatus() { return status.get(); }
        public String getType() { return type; }
        public int getTotalInt() { return totalInt; }
        public Document getOriginalDocument() { return originalDocument; }
    }
}