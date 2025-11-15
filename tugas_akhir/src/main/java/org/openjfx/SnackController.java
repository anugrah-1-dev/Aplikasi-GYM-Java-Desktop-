package org.openjfx;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

// Import untuk printing
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

public class SnackController implements Initializable {

    @FXML private Label clockLabel;
    @FXML private TextField transactionIdField;
    @FXML private TextField customerNameField;
    @FXML private DatePicker transactionDateField;
    @FXML private TextField totalPaymentField;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalItemsLabel;
    @FXML private Button generateIdButton;
    @FXML private Button processButton;
    @FXML private Button resetButton;
    @FXML private Button backButton;
    @FXML private GridPane productGrid;
    
    // Payment fields
    @FXML private TextField cashGivenField;
    @FXML private TextField changeField;
    @FXML private Label paymentStatusLabel;
    @FXML private Button calculateChangeButton;

    // Data structures
    private Map<String, Integer> productQuantities;
    private Map<String, SnackProduct> productsMap;
    private Map<String, Label> quantityLabels;
    private MongoDBService mongoDBService;

    // Payment variables
    private double totalPaymentAmount = 0;
    private double cashGivenAmount = 0;
    private double changeAmount = 0;

    // Printer service
    private PrintService printerService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        testImageLoading();
        mongoDBService = MongoDBService.getInstance();
        initializeData();
        loadProductsFromDatabase();
        initializeClock();
        setupEventHandlers();
        generateNewTransactionId();
        transactionDateField.setValue(LocalDate.now());
        setupPaymentFields();
        populateProductGrid();
        initializePrinter();
    }

    private void initializePrinter() {
        try {
            // Cari printer Xantre POS-58
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            printerService = null;
            
            for (PrintService service : services) {
                System.out.println("Printer ditemukan: " + service.getName());
                if (service.getName().toLowerCase().contains("xantre") || 
                    service.getName().toLowerCase().contains("pos-58") ||
                    service.getName().toLowerCase().contains("pos58") ||
                    service.getName().toLowerCase().contains("thermal")) {
                    printerService = service;
                    System.out.println("Printer Xantre POS-58 ditemukan: " + service.getName());
                    break;
                }
            }
            
            if (printerService == null && services.length > 0) {
                // Gunakan printer default jika Xantre tidak ditemukan
                printerService = PrintServiceLookup.lookupDefaultPrintService();
                System.out.println("Menggunakan printer default: " + (printerService != null ? printerService.getName() : "Tidak ada"));
            }
            
        } catch (Exception e) {
            System.err.println("Error initializing printer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testImageLoading() {
        System.out.println("=== Testing Image Loading ===");
        String[] testImages = {
            "/org/icon/protein-bar.jpg",
            "/org/icon/energy-drink.jpg", 
            "/org/icon/healthy-snack.jpg",
            "/org/icon/aqua-besar.jpg",
            "/org/icon/aqua.jpg",
            "/org/icon/le-minerale-besar.jpg",
            "/org/icon/le-minerale-kecil.jpg",
            "/org/icon/vit-besar.jpg",
            "/org/icon/vit-kecil.jpg",
            "/org/icon/pristine-besar.jpg",
            "/org/icon/pristine-kecil.jpg",
            "/org/icon/mineral-water.jpg",
            "/org/icon/vitamin.jpg",
            "/org/icon/sports-drink.jpg",
            "/org/icon/energy-gel.jpg",
            "/org/icon/protein-shake.jpg",
            "/org/icon/default-product.jpg"
        };
        
        for (String imagePath : testImages) {
            try {
                URL url = getClass().getResource(imagePath);
                if (url != null) {
                    System.out.println("✓ Found: " + imagePath);
                    Image testImage = new Image(url.toString());
                    if (!testImage.isError()) {
                        System.out.println("  ✓ Image loaded successfully");
                    } else {
                        System.err.println("  ✗ Image loading error");
                    }
                } else {
                    System.err.println("✗ Missing: " + imagePath);
                }
            } catch (Exception e) {
                System.err.println("✗ Error: " + imagePath + " - " + e.getMessage());
            }
        }
        System.out.println("=== Image Test Complete ===");
    }

    private void initializeData() {
        productQuantities = new HashMap<>();
        productsMap = new HashMap<>();
        quantityLabels = new HashMap<>();
    }

    private void loadProductsFromDatabase() {
        try {
            List<SnackProduct> products = mongoDBService.getAllProducts();
            
            if (products.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", 
                    "Tidak ada produk di database. Pastikan koleksi data_snack sudah terisi.");
                return;
            }
            
            for (SnackProduct product : products) {
                productsMap.put(product.getProductName(), product);
                productQuantities.put(product.getProductName(), 0);
                System.out.println("Loaded product: " + product.getProductName() + " - Stock: " + product.getStock());
            }
            
            System.out.println("Successfully loaded " + products.size() + " products from database");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Gagal memuat produk dari database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateProductGrid() {
        productGrid.getChildren().clear();
        productGrid.getRowConstraints().clear();
        
        List<SnackProduct> products = new ArrayList<>(productsMap.values());
        
        int row = 0;
        int col = 0;
        int maxColumns = 4;
        
        for (int i = 0; i < products.size(); i++) {
            SnackProduct product = products.get(i);
            VBox productCard = createProductCard(product);
            productGrid.add(productCard, col, row);
            
            col++;
            if (col >= maxColumns) {
                col = 0;
                row++;
                javafx.scene.layout.RowConstraints rowConstraint = new javafx.scene.layout.RowConstraints();
                rowConstraint.setPrefHeight(220.0);
                productGrid.getRowConstraints().add(rowConstraint);
            }
        }
        
        System.out.println("Product grid populated with " + products.size() + " products");
    }

    private VBox createProductCard(SnackProduct product) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setSpacing(12);
        card.setPrefWidth(260);
        card.setPrefHeight(200);
        
        ImageView imageView = createProductImageView(product);
        
        Label nameLabel = new Label(product.getProductName());
        nameLabel.getStyleClass().add("product-name");
        
        Label priceLabel = new Label(String.format("Rp %,.0f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");
        
        Label stockLabel = new Label("Stok: " + product.getStock());
        stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        
        HBox quantityControls = new HBox();
        quantityControls.getStyleClass().add("quantity-controls");
        quantityControls.setSpacing(10);
        
        Button decrementBtn = new Button("-");
        decrementBtn.getStyleClass().add("quantity-btn");
        decrementBtn.setOnAction(e -> decrementQuantity(product.getProductName()));
        
        Label quantityLabel = new Label("0");
        quantityLabel.getStyleClass().add("quantity-label");
        quantityLabels.put(product.getProductName(), quantityLabel);
        
        Button incrementBtn = new Button("+");
        incrementBtn.getStyleClass().add("quantity-btn");
        incrementBtn.setOnAction(e -> incrementQuantity(product.getProductName()));
        
        quantityControls.getChildren().addAll(decrementBtn, quantityLabel, incrementBtn);
        
        card.getChildren().addAll(imageView, nameLabel, priceLabel, stockLabel, quantityControls);
        
        return card;
    }

    private ImageView createProductImageView(SnackProduct product) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(80);
        imageView.setFitWidth(80);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("product-image");
        
        String imagePath = getStaticImagePath(product.getProductName());
        System.out.println("Loading image for: " + product.getProductName() + " -> " + imagePath);
        
        try {
            URL imageUrl = getClass().getResource(imagePath);
            if (imageUrl != null) {
                Image image = new Image(imageUrl.toString());
                if (!image.isError()) {
                    imageView.setImage(image);
                    System.out.println("✓ Successfully loaded image for: " + product.getProductName());
                } else {
                    System.err.println("✗ Image error for: " + product.getProductName());
                    loadFallbackImage(imageView, product.getProductName());
                }
            } else {
                System.err.println("✗ Image URL not found for: " + product.getProductName() + " at path: " + imagePath);
                loadFallbackImage(imageView, product.getProductName());
            }
        } catch (Exception e) {
            System.err.println("✗ Exception loading image for " + product.getProductName() + ": " + e.getMessage());
            loadFallbackImage(imageView, product.getProductName());
        }
        
        return imageView;
    }

    private String getStaticImagePath(String productName) {
        Map<String, String> imageMap = new HashMap<>();
        
        imageMap.put("Le Minerale Besar", "/org/icon/le-minerale-besar.jpg");
        imageMap.put("Le Minerale Kecil", "/org/icon/le-minerale-kecil.jpg");
        imageMap.put("Aqua Besar", "/org/icon/aqua-besar.jpg");
        imageMap.put("Aqua Kecil", "/org/icon/aqua.jpg");
        imageMap.put("Vit Besar", "/org/icon/vit-besar.jpg");
        imageMap.put("Vit Kecil", "/org/icon/vit-kecil.jpg");
        imageMap.put("Pristine Besar", "/org/icon/pristine-besar.jpg");
        imageMap.put("Pristine Kecil", "/org/icon/pristine-kecil.jpg");
        
        imageMap.put("Protein Bar", "/org/icon/protein-bar.jpg");
        imageMap.put("Energy Drink", "/org/icon/energy-drink.jpg");
        imageMap.put("Healthy Snack", "/org/icon/healthy-snack.jpg");
        imageMap.put("Mineral Water", "/org/icon/mineral-water.jpg");
        imageMap.put("Vitamin Pack", "/org/icon/vitamin.jpg");
        imageMap.put("Sports Drink", "/org/icon/sports-drink.jpg");
        imageMap.put("Energy Gel", "/org/icon/energy-gel.jpg");
        imageMap.put("Protein Shake", "/org/icon/protein-shake.jpg");
        
        return imageMap.getOrDefault(productName, "/org/icon/default-product.jpg");
    }

    private void loadFallbackImage(ImageView imageView, String productName) {
        try {
            URL fallbackUrl = getClass().getResource("/org/icon/default-product.jpg");
            if (fallbackUrl != null) {
                Image fallbackImage = new Image(fallbackUrl.toString());
                if (!fallbackImage.isError()) {
                    imageView.setImage(fallbackImage);
                    System.out.println("✓ Loaded fallback image for: " + productName);
                } else {
                    createPlaceholderImage(imageView, productName);
                }
            } else {
                createPlaceholderImage(imageView, productName);
            }
        } catch (Exception e) {
            createPlaceholderImage(imageView, productName);
        }
    }

    private void createPlaceholderImage(ImageView imageView, String productName) {
        imageView.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-radius: 5;");
        System.err.println("✗ All image loading failed for: " + productName + ", using placeholder");
    }

    private void setupPaymentFields() {
        TextFormatter<String> cashFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        });
        cashGivenField.setTextFormatter(cashFormatter);
        
        clearPaymentFields();
    }

    private void clearPaymentFields() {
        cashGivenField.clear();
        changeField.clear();
        paymentStatusLabel.setText("Menunggu Pembayaran");
        paymentStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-border-color: #f39c12; -fx-background-color: #fffaf0;");
        cashGivenAmount = 0;
        changeAmount = 0;
    }

    private void initializeClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            clockLabel.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void setupEventHandlers() {
        generateIdButton.setOnAction(e -> generateNewTransactionId());
        processButton.setOnAction(e -> processTransaction());
        resetButton.setOnAction(e -> resetForm());
        backButton.setOnAction(e -> goBack());
        calculateChangeButton.setOnAction(e -> calculateChange());
        
        cashGivenField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                calculateChange();
            } else {
                changeField.clear();
                paymentStatusLabel.setText("Menunggu Pembayaran");
                paymentStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-border-color: #f39c12; -fx-background-color: #fffaf0;");
            }
        });
    }

    private void incrementQuantity(String productName) {
        SnackProduct product = productsMap.get(productName);
        if (product == null) {
            System.err.println("Product not found: " + productName);
            return;
        }
        
        int currentQuantity = productQuantities.getOrDefault(productName, 0);
        
        if (currentQuantity < product.getStock()) {
            productQuantities.put(productName, currentQuantity + 1);
            updateQuantityDisplay(productName);
            calculateTotals();
            System.out.println("Incremented " + productName + " to " + (currentQuantity + 1));
        } else {
            showAlert(Alert.AlertType.WARNING, "Stok Tidak Cukup", 
                "Stok " + productName + " tidak mencukupi!\nStok tersedia: " + product.getStock());
        }
    }

    private void decrementQuantity(String productName) {
        int currentQuantity = productQuantities.getOrDefault(productName, 0);
        if (currentQuantity > 0) {
            productQuantities.put(productName, currentQuantity - 1);
            updateQuantityDisplay(productName);
            calculateTotals();
            System.out.println("Decremented " + productName + " to " + (currentQuantity - 1));
        }
    }

    private void updateQuantityDisplay(String productName) {
        Label quantityLabel = quantityLabels.get(productName);
        if (quantityLabel != null) {
            quantityLabel.setText(String.valueOf(productQuantities.get(productName)));
        }
    }

    private void calculateTotals() {
        double subtotal = 0;
        int totalItems = 0;
        
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productName = entry.getKey();
            int quantity = entry.getValue();
            SnackProduct product = productsMap.get(productName);
            
            if (product != null && quantity > 0) {
                double itemTotal = product.getPrice() * quantity;
                subtotal += itemTotal;
                totalItems += quantity;
            }
        }
        
        // Tidak ada pajak untuk semua produk
        totalPaymentAmount = subtotal;
        
        updateDisplayTotals(subtotal, 0, totalPaymentAmount, totalItems);
        clearPaymentFields();
        
        System.out.println("Calculated totals - Subtotal: " + subtotal + ", Total: " + totalPaymentAmount + ", Items: " + totalItems);
    }

    private void updateDisplayTotals(double subtotal, double tax, double totalPayment, int totalItems) {
        subtotalLabel.setText(String.format("Rp %,.0f", subtotal));
        taxLabel.setText(String.format("Rp %,.0f", tax));
        totalPaymentField.setText(String.format("Rp %,.0f", totalPayment));
        totalItemsLabel.setText(totalItems + " Item");
    }

    @FXML
    private void calculateChange() {
        try {
            if (cashGivenField.getText().isEmpty()) {
                changeField.clear();
                paymentStatusLabel.setText("Menunggu Pembayaran");
                paymentStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-border-color: #f39c12; -fx-background-color: #fffaf0;");
                return;
            }
            
            cashGivenAmount = Double.parseDouble(cashGivenField.getText());
            changeAmount = cashGivenAmount - totalPaymentAmount;
            
            if (changeAmount >= 0) {
                changeField.setText(String.format("Rp %,.0f", changeAmount));
                paymentStatusLabel.setText("Pembayaran Lengkap");
                paymentStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-border-color: #27ae60; -fx-background-color: #f0fff4;");
            } else {
                double shortage = Math.abs(changeAmount);
                changeField.setText(String.format("Rp -%,.0f", shortage));
                paymentStatusLabel.setText("Uang Kurang");
                paymentStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-background-color: #fdf2f2;");
            }
            
            System.out.println("Change calculated - Cash: " + cashGivenAmount + ", Change: " + changeAmount);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Masukkan jumlah uang yang valid!");
            cashGivenField.requestFocus();
        }
    }

    private void generateNewTransactionId() {
        String newTransactionId = mongoDBService.getNextTransactionId();
        transactionIdField.setText(newTransactionId);
        System.out.println("Generated new transaction ID: " + newTransactionId);
    }

    private void processTransaction() {
        if (customerNameField.getText() == null || customerNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Nama pelanggan harus diisi!");
            customerNameField.requestFocus();
            return;
        }
        
        if (transactionDateField.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Tanggal pembelian harus dipilih!");
            transactionDateField.requestFocus();
            return;
        }
        
        int totalItems = 0;
        for (int quantity : productQuantities.values()) {
            totalItems += quantity;
        }
        
        if (totalItems == 0) {
            showAlert(Alert.AlertType.ERROR, "Error", "Pilih minimal 1 produk snack!");
            return;
        }

        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productName = entry.getKey();
            int quantity = entry.getValue();
            
            if (quantity > 0) {
                SnackProduct product = productsMap.get(productName);
                if (product.getStock() < quantity) {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                        "Stok tidak cukup untuk " + productName + "!\n" +
                        "Stok tersedia: " + product.getStock() + "\n" +
                        "Jumlah diminta: " + quantity);
                    return;
                }
            }
        }

        if (cashGivenField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Masukkan jumlah uang yang diberikan!");
            cashGivenField.requestFocus();
            return;
        }

        if (changeAmount < 0) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Uang yang diberikan tidak cukup!\n" +
                "Kekurangan: " + String.format("Rp %,.0f", Math.abs(changeAmount)));
            cashGivenField.requestFocus();
            return;
        }

        try {
            TransactionSnack transaction = new TransactionSnack(
                transactionIdField.getText(),
                customerNameField.getText().trim(),
                transactionDateField.getValue()
            );

            transaction.setCashGiven(cashGivenAmount);
            transaction.setChange(changeAmount);
            transaction.setPaymentStatus("Completed");

            for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
                String productName = entry.getKey();
                int quantity = entry.getValue();
                
                if (quantity > 0) {
                    SnackProduct product = productsMap.get(productName);
                    transaction.addItem(
                        product.getProductId(),
                        productName, 
                        quantity, 
                        product.getPrice()
                    );
                }
            }

            transaction.calculateTotals();

            boolean success = mongoDBService.saveTransaction(transaction);
            
            if (success) {
                for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
                    String productName = entry.getKey();
                    int quantity = entry.getValue();
                    
                    if (quantity > 0) {
                        SnackProduct product = productsMap.get(productName);
                        mongoDBService.updateStock(product.getProductId(), quantity);
                    }
                }
                
                // Tampilkan dialog konfirmasi cetak struk
                showPrintConfirmationDialog(transaction);
                
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan transaksi ke database!");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showPrintConfirmationDialog(TransactionSnack transaction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Transaksi Berhasil");
        alert.setHeaderText("Transaksi berhasil disimpan!");
        alert.setContentText(
            "ID Transaksi: " + transaction.getTransactionId() + "\n" +
            "Total Pembayaran: " + String.format("Rp %,.0f", transaction.getTotalPayment()) + "\n" +
            "Uang Diberikan: " + String.format("Rp %,.0f", transaction.getCashGiven()) + "\n" +
            "Kembalian: " + String.format("Rp %,.0f", transaction.getChange()) + "\n\n" +
            "Apakah Anda ingin mencetak struk?"
        );

        ButtonType printButton = new ButtonType("Cetak Struk");
        ButtonType noPrintButton = new ButtonType("Tidak Cetak");
        ButtonType cancelButton = new ButtonType("Batal", ButtonType.CANCEL.getButtonData());

        alert.getButtonTypes().setAll(printButton, noPrintButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == printButton) {
                boolean printSuccess = printReceipt(transaction);
                if (printSuccess) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Struk berhasil dicetak!");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Peringatan", 
                        "Transaksi berhasil tetapi gagal mencetak struk.\n" +
                        "Silakan periksa koneksi printer.");
                }
                resetForm();
                generateNewTransactionId();
                loadProductsFromDatabase();
                populateProductGrid();
            } else if (response == noPrintButton) {
                resetForm();
                generateNewTransactionId();
                loadProductsFromDatabase();
                populateProductGrid();
            }
            // Jika cancel, tidak melakukan apa-apa
        });
    }

    private boolean printReceipt(TransactionSnack transaction) {
        try {
            if (printerService == null) {
                System.err.println("Printer service not available");
                return false;
            }

            // Format struk untuk printer thermal
            StringBuilder receipt = new StringBuilder();
            
            // Reset printer
            receipt.append("\u001B@");
            
            // Header - Center align dan bold
            receipt.append("\u001B").append("a").append("\u0001"); // Center align
            receipt.append("\u001B").append("E").append("\u0001"); // Bold on
            receipt.append("GYM SNACK BAR\n");
            receipt.append("\u001B").append("E").append("\u0000"); // Bold off
            receipt.append("Jl. Fitness Center No. 123\n");
            receipt.append("Telp: (021) 1234-5678\n");
            receipt.append("\u001B").append("a").append("\u0000"); // Left align
            
            // Garis pemisah
            receipt.append("--------------------------------\n");
            
            // Informasi transaksi
            receipt.append("ID Transaksi : ").append(transaction.getTransactionId()).append("\n");
            receipt.append("Pelanggan    : ").append(transaction.getCustomerName()).append("\n");
            receipt.append("Tanggal      : ").append(transaction.getTransactionDate()).append("\n");
            receipt.append("Waktu        : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n");
            receipt.append("--------------------------------\n");
            
            // Header item
            receipt.append("\u001B").append("E").append("\u0001"); // Bold on
            receipt.append(String.format("%-16s %2s %10s\n", "ITEM", "QTY", "TOTAL"));
            receipt.append("\u001B").append("E").append("\u0000"); // Bold off
            
            // Detail item
            for (Map.Entry<String, Integer> entry : transaction.getItems().entrySet()) {
                String productName = entry.getKey();
                int quantity = entry.getValue();
                double price = transaction.getProductPrices().get(productName);
                double total = price * quantity;
                
                // Potong nama produk jika terlalu panjang
                String displayName = productName.length() > 15 ? productName.substring(0, 15) : productName;
                
                receipt.append(String.format("%-15s %3d %10.0f\n", displayName, quantity, total));
            }
            
            receipt.append("--------------------------------\n");
            
            // Total dan pembayaran - TANPA PAJAK
            receipt.append(String.format("%-18s %11.0f\n", "Subtotal:", transaction.getSubtotal()));
            
            receipt.append("\u001B").append("E").append("\u0001"); // Bold on untuk total
            receipt.append(String.format("%-18s %11.0f\n", "TOTAL:", transaction.getTotalPayment()));
            receipt.append("\u001B").append("E").append("\u0000"); // Bold off
            
            receipt.append("--------------------------------\n");
            
            receipt.append(String.format("%-18s %11.0f\n", "TUNAI:", transaction.getCashGiven()));
            receipt.append(String.format("%-18s %11.0f\n", "KEMBALI:", transaction.getChange()));
            receipt.append("================================\n");
            
            // Informasi WiFi - Center align
            receipt.append("\u001B").append("a").append("\u0001"); // Center align
            receipt.append("\u001B").append("E").append("\u0001"); // Bold on
            receipt.append("FREE WiFi ACCESS\n");
            receipt.append("\u001B").append("E").append("\u0000"); // Bold off
            receipt.append("BIFVIT24 FITNESS 5G\n");
            receipt.append("Password: MERDEKA45\n");
            receipt.append("BIFVIT24 FITNESS 4G\n");
            receipt.append("Password: MERDEKA45\n");
            receipt.append("\u001B").append("a").append("\u0000"); // Left align
            
            receipt.append("--------------------------------\n");
            
            // Footer - Center align
            receipt.append("\u001B").append("a").append("\u0001"); // Center align
            receipt.append("Terima kasih atas kunjungan Anda!\n");
            receipt.append("Semoga latihan Anda menyenangkan!\n\n");
            receipt.append("Struk ini sebagai bukti pembayaran\n");
            receipt.append("yang sah\n");
            receipt.append("\u001B").append("a").append("\u0000"); // Left align
            
            // Feed paper dan cut
            receipt.append("\n\n\n");
            receipt.append("\u001D").append("V").append("\u0041"); // Partial cut
            receipt.append("\u0003"); // Feed 3 lines

            // Convert to bytes dengan encoding yang tepat
            byte[] receiptBytes = receipt.toString().getBytes("ISO-8859-1");

            // Create print job
            DocPrintJob job = printerService.createPrintJob();
            SimpleDoc doc = new SimpleDoc(receiptBytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            
            // Set print attributes
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            attributes.add(new Copies(1));
            attributes.add(MediaSizeName.ISO_A4);
            attributes.add(OrientationRequested.PORTRAIT);
            
            // Print the receipt
            job.print(doc, attributes);
            
            System.out.println("Receipt printed successfully for transaction: " + transaction.getTransactionId());
            return true;
            
        } catch (PrintException e) {
            System.err.println("Print error: " + e.getMessage());
            e.printStackTrace();
            // Fallback ke console print
            printReceiptToConsole(transaction);
            return false;
        } catch (Exception e) {
            System.err.println("Error generating receipt: " + e.getMessage());
            e.printStackTrace();
            // Fallback ke console print
            printReceiptToConsole(transaction);
            return false;
        }
    }

    private void printReceiptToConsole(TransactionSnack transaction) {
        System.out.println("\n=================================");
        System.out.println("       GYM SNACK BAR RECEIPT     ");
        System.out.println("=================================");
        System.out.println("ID Transaksi : " + transaction.getTransactionId());
        System.out.println("Pelanggan    : " + transaction.getCustomerName());
        System.out.println("Tanggal      : " + transaction.getTransactionDate());
        System.out.println("Waktu        : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        System.out.println("---------------------------------");
        System.out.println("ITEM               QTY     TOTAL");
        System.out.println("---------------------------------");
        
        for (Map.Entry<String, Integer> entry : transaction.getItems().entrySet()) {
            String productName = entry.getKey();
            int quantity = entry.getValue();
            double price = transaction.getProductPrices().get(productName);
            double total = price * quantity;
            
            System.out.printf("%-18s %3d %10.0f%n", 
                productName, quantity, total);
        }
        
        System.out.println("---------------------------------");
        System.out.printf("%-18s %11.0f%n", "Subtotal:", transaction.getSubtotal());
        System.out.printf("%-18s %11.0f%n", "TOTAL:", transaction.getTotalPayment());
        System.out.println("---------------------------------");
        System.out.printf("%-18s %11.0f%n", "TUNAI:", transaction.getCashGiven());
        System.out.printf("%-18s %11.0f%n", "KEMBALI:", transaction.getChange());
        System.out.println("=================================");
        System.out.println("         FREE WiFi ACCESS        ");
        System.out.println("BIFVIT24 FITNESS 5G");
        System.out.println("Password: MERDEKA45");
        System.out.println("BIFVIT24 FITNESS 4G");
        System.out.println("Password: MERDEKA45");
        System.out.println("=================================");
        System.out.println("     TERIMA KASIH ATAS KUNJUNGAN ANDA");
        System.out.println("=================================\n");
    }

    private void resetForm() {
        customerNameField.clear();
        transactionDateField.setValue(LocalDate.now());
        
        for (String productName : productQuantities.keySet()) {
            productQuantities.put(productName, 0);
            updateQuantityDisplay(productName);
        }
        
        updateDisplayTotals(0, 0, 0, 0);
        clearPaymentFields();
        totalPaymentAmount = 0;
        
        System.out.println("Form reset successfully");
    }

    private void goBack() {
        try {
            System.out.println("Navigating back to menu transaksi...");
            
            resetForm();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_transaksi/menu_transaksi.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Menu Transaksi");
            stage.show();
            
            System.out.println("Successfully navigated to menu transaksi");
            
        } catch (IOException e) {
            System.err.println("Error loading menu_transaksi.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Gagal kembali ke menu transaksi: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (mongoDBService != null) {
            mongoDBService.closeConnection();
        }
    }

    // ========== INNER CLASSES ==========

    public static class SnackProduct {
        private ObjectId id;
        private String productId;
        private String productName;
        private double price;
        private int stock;
        private String category;
        private String description;
        private boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public SnackProduct() {
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
            this.isActive = true;
        }

        // Getters and Setters
        public ObjectId getId() { return id; }
        public void setId(ObjectId id) { this.id = id; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public int getStock() { return stock; }
        public void setStock(int stock) { this.stock = stock; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        @Override
        public String toString() {
            return "SnackProduct{" +
                    "productId='" + productId + '\'' +
                    ", productName='" + productName + '\'' +
                    ", price=" + price +
                    ", stock=" + stock +
                    ", category=" + category +
                    '}';
        }
    }

    public static class TransactionSnack {
        private ObjectId id;
        private String transactionId;
        private String customerName;
        private LocalDate transactionDate;
        private LocalDateTime createdAt;
        private Map<String, Integer> items;
        private Map<String, String> productIds;
        private Map<String, Double> productPrices;
        private double subtotal;
        private double tax;
        private double totalPayment;
        private int totalItems;
        private String status;
        private double cashGiven;
        private double change;
        private String paymentStatus;

        public TransactionSnack() {
            this.items = new HashMap<>();
            this.productIds = new HashMap<>();
            this.productPrices = new HashMap<>();
            this.createdAt = LocalDateTime.now();
            this.status = "Completed";
            this.paymentStatus = "Pending";
            this.tax = 0; // Tidak ada pajak
        }

        public TransactionSnack(String transactionId, String customerName, LocalDate transactionDate) {
            this();
            this.transactionId = transactionId;
            this.customerName = customerName;
            this.transactionDate = transactionDate;
        }

        public void addItem(String productId, String productName, int quantity, double price) {
            this.items.put(productName, quantity);
            this.productIds.put(productName, productId);
            this.productPrices.put(productName, price);
        }

        public void calculateTotals() {
            this.subtotal = 0;
            this.totalItems = 0;
            this.tax = 0; // Tidak ada pajak untuk semua produk
            
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                String productName = entry.getKey();
                int quantity = entry.getValue();
                double price = productPrices.get(productName);
                
                double itemTotal = price * quantity;
                this.subtotal += itemTotal;
                this.totalItems += quantity;
            }
            
            // Total payment sama dengan subtotal (tanpa pajak)
            this.totalPayment = this.subtotal;
        }

        // Getters and Setters
        public ObjectId getId() { return id; }
        public void setId(ObjectId id) { this.id = id; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public LocalDate getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public Map<String, Integer> getItems() { return items; }
        public void setItems(Map<String, Integer> items) { this.items = items; }
        public Map<String, String> getProductIds() { return productIds; }
        public void setProductIds(Map<String, String> productIds) { this.productIds = productIds; }
        public Map<String, Double> getProductPrices() { return productPrices; }
        public void setProductPrices(Map<String, Double> productPrices) { this.productPrices = productPrices; }
        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
        public double getTax() { return tax; }
        public void setTax(double tax) { this.tax = tax; }
        public double getTotalPayment() { return totalPayment; }
        public void setTotalPayment(double totalPayment) { this.totalPayment = totalPayment; }
        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getCashGiven() { return cashGiven; }
        public void setCashGiven(double cashGiven) { this.cashGiven = cashGiven; }
        public double getChange() { return change; }
        public void setChange(double change) { this.change = change; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

        @Override
        public String toString() {
            return "TransactionSnack{" +
                    "transactionId='" + transactionId + '\'' +
                    ", customerName='" + customerName + '\'' +
                    ", totalPayment=" + totalPayment +
                    ", totalItems=" + totalItems +
                    ", tax=" + tax +
                    '}';
        }
    }

    public static class MongoDBService {
        private static MongoDBService instance;
        private MongoClient mongoClient;
        private MongoDatabase database;
        private MongoCollection<Document> transactionCollection;
        private MongoCollection<Document> productCollection;

        private MongoDBService() {
            try {
                String connectionString = "mongodb://localhost:27017";
                mongoClient = MongoClients.create(connectionString);
                database = mongoClient.getDatabase("gym");
                transactionCollection = database.getCollection("transaksi_snack");
                productCollection = database.getCollection("data_snack");
                System.out.println("Connected to MongoDB successfully");
                
                initializeSnackDataIfEmpty();
            } catch (Exception e) {
                System.err.println("Error connecting to MongoDB: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public static MongoDBService getInstance() {
            if (instance == null) {
                instance = new MongoDBService();
            }
            return instance;
        }

        private void initializeSnackDataIfEmpty() {
            try {
                long productCount = productCollection.countDocuments();
                if (productCount == 0) {
                    System.out.println("Initializing default snack data...");
                    
                    List<Document> defaultProducts = new ArrayList<>();
                    
                    // Semua produk tanpa pajak
                    defaultProducts.add(createProductDocument("SNK-001", "Protein Bar", 25000, 99, "Protein", "High protein bar untuk pemulihan otot", "/org/icon/protein-bar.jpg"));
                    defaultProducts.add(createProductDocument("SNK-002", "Energy Drink", 18000, 149, "Beverage", "Minuman energi untuk stamina maksimal!", "/org/icon/energy-drink.jpg"));
                    defaultProducts.add(createProductDocument("SNK-003", "Healthy Snack", 15000, 75, "Snack", "Snack sehat rendah kalori", "/org/icon/healthy-snack.jpg"));
                    defaultProducts.add(createProductDocument("SNK-004", "Mineral Water", 5000, 200, "Beverage", "Air mineral segar", "/org/icon/mineral-water.jpg"));
                    defaultProducts.add(createProductDocument("SNK-005", "Vitamin Pack", 35000, 50, "Supplement", "Paket vitamin lengkap", "/org/icon/vitamin.jpg"));
                    defaultProducts.add(createProductDocument("SNK-006", "Sports Drink", 20000, 80, "Beverage", "Minuman olahraga", "/org/icon/sports-drink.jpg"));
                    defaultProducts.add(createProductDocument("SNK-007", "Energy Gel", 12000, 60, "Supplement", "Gel energi instant", "/org/icon/energy-gel.jpg"));
                    defaultProducts.add(createProductDocument("SNK-008", "Protein Shake", 32000, 45, "Protein", "Shake protein tinggi", "/org/icon/protein-shake.jpg"));
                    
                    // Minuman products - juga tanpa pajak
                    defaultProducts.add(createProductDocument("MINUMAN-001", "Le Minerale Besar", 7000, 50, "Air Mineral", "Le Minerale ukuran 1500ml", "/org/icon/le-minerale-besar.jpg"));
                    defaultProducts.add(createProductDocument("MINUMAN-002", "Le Minerale Kecil", 4000, 60, "Air Mineral", "Le Minerale ukuran 600ml", "/org/icon/le-minerale-kecil.jpg"));
                    defaultProducts.add(createProductDocument("MINUMAN-003", "Aqua Besar", 6500, 45, "Air Mineral", "Aqua ukuran 1500ml", "/org/icon/aqua-besar.jpg"));
                    defaultProducts.add(createProductDocument("MINUMAN-004", "Aqua Kecil", 3500, 70, "Air Mineral", "Aqua ukuran 600ml", "/org/icon/aqua.jpg"));
                    defaultProducts.add(createProductDocument("MINUMAN-005", "Vit Besar", 7500, 40, "Air Mineral", "Vit ukuran 1500ml", "/org/icon/vit-besar.jpg"));
                    defaultProducts.add(createProductDocument("MINUMAN-006", "Vit Kecil", 4500, 55, "Air Mineral", "Vit ukuran 600ml", "/org/icon/vit-kecil.jpg"));
                    defaultProducts.add(createProductDocument("MINUMAN-007", "Pristine Besar", 8000, 35, "Air Mineral", "Pristine ukuran 1500ml", "/org/icon/pristine-besar.jpg"));
                    defaultProducts.add(createProductDocument("MINUMAN-008", "Pristine Kecil", 5000, 50, "Air Mineral", "Pristine ukuran 600ml", "/org/icon/pristine-kecil.jpg"));
                    
                    productCollection.insertMany(defaultProducts);
                    System.out.println("Default snack data initialized successfully with " + defaultProducts.size() + " products");
                } else {
                    System.out.println("Snack data already exists: " + productCount + " products");
                }
            } catch (Exception e) {
                System.err.println("Error initializing snack data: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private Document createProductDocument(String productId, String productName, double price, int stock, 
                                             String category, String description, String imageUrl) {
            return new Document()
                .append("productId", productId)
                .append("productName", productName)
                .append("price", price)
                .append("stock", stock)
                .append("category", category)
                .append("description", description)
                .append("imageUrl", imageUrl)
                .append("isActive", true)
                .append("createdAt", new Date())
                .append("updatedAt", new Date());
        }

        public List<SnackProduct> getAllProducts() {
            List<SnackProduct> products = new ArrayList<>();
            try {
                Document filter = new Document("isActive", true);
                for (Document doc : productCollection.find(filter)) {
                    SnackProduct product = documentToProduct(doc);
                    products.add(product);
                }
                System.out.println("Fetched " + products.size() + " active products from database");
            } catch (Exception e) {
                System.err.println("Error fetching products: " + e.getMessage());
                e.printStackTrace();
            }
            return products;
        }

        public SnackProduct getProductById(String productId) {
            try {
                Document doc = productCollection.find(eq("productId", productId)).first();
                return doc != null ? documentToProduct(doc) : null;
            } catch (Exception e) {
                System.err.println("Error fetching product: " + e.getMessage());
                return null;
            }
        }

        public boolean updateStock(String productId, int quantitySold) {
            try {
                Document product = productCollection.find(eq("productId", productId)).first();
                if (product != null) {
                    int currentStock = product.getInteger("stock");
                    int newStock = currentStock - quantitySold;
                    
                    if (newStock < 0) {
                        System.err.println("Insufficient stock for product: " + productId);
                        return false;
                    }
                    
                    Document update = new Document("$set", 
                        new Document("stock", newStock)
                            .append("updatedAt", new Date()));
                    
                    productCollection.updateOne(eq("productId", productId), update);
                    System.out.println("Stock updated for " + productId + ": " + currentStock + " -> " + newStock);
                    return true;
                }
                System.err.println("Product not found: " + productId);
                return false;
            } catch (Exception e) {
                System.err.println("Error updating stock: " + e.getMessage());
                return false;
            }
        }

        // ========== STOCK MANAGEMENT METHODS ==========

        public boolean addProduct(SnackProduct product) {
            try {
                Document doc = new Document()
                    .append("productId", product.getProductId())
                    .append("productName", product.getProductName())
                    .append("price", product.getPrice())
                    .append("stock", product.getStock())
                    .append("category", product.getCategory())
                    .append("description", product.getDescription())
                    .append("isActive", product.isActive())
                    .append("createdAt", new Date())
                    .append("updatedAt", new Date());

                InsertOneResult result = productCollection.insertOne(doc);
                boolean success = result.wasAcknowledged();
                System.out.println("Product added: " + success + " - ID: " + product.getProductId());
                return success;
            } catch (Exception e) {
                System.err.println("Error adding product: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        public boolean updateProduct(SnackProduct product) {
            try {
                Document update = new Document("$set", 
                    new Document("productName", product.getProductName())
                        .append("price", product.getPrice())
                        .append("stock", product.getStock())
                        .append("category", product.getCategory())
                        .append("description", product.getDescription())
                        .append("updatedAt", new Date()));

                UpdateResult result = productCollection.updateOne(
                    eq("productId", product.getProductId()), update);
                
                boolean success = result.wasAcknowledged() && result.getModifiedCount() > 0;
                System.out.println("Product updated: " + success + " - ID: " + product.getProductId());
                return success;
            } catch (Exception e) {
                System.err.println("Error updating product: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        public boolean setStock(String productId, int newStock) {
            try {
                Document update = new Document("$set", 
                    new Document("stock", newStock)
                        .append("updatedAt", new Date()));

                UpdateResult result = productCollection.updateOne(
                    eq("productId", productId), update);
                
                boolean success = result.wasAcknowledged() && result.getModifiedCount() > 0;
                System.out.println("Stock set: " + success + " - Product: " + productId + " -> " + newStock);
                return success;
            } catch (Exception e) {
                System.err.println("Error setting stock: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        public boolean deleteProduct(String productId) {
            try {
                // Soft delete - set isActive to false
                Document update = new Document("$set", 
                    new Document("isActive", false)
                        .append("updatedAt", new Date()));

                UpdateResult result = productCollection.updateOne(
                    eq("productId", productId), update);
                
                boolean success = result.wasAcknowledged() && result.getModifiedCount() > 0;
                System.out.println("Product deleted (soft): " + success + " - ID: " + productId);
                return success;
            } catch (Exception e) {
                System.err.println("Error deleting product: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        public SnackProduct getProductByProductId(String productId) {
            try {
                Document doc = productCollection.find(eq("productId", productId)).first();
                return doc != null ? documentToProduct(doc) : null;
            } catch (Exception e) {
                System.err.println("Error fetching product by ID: " + e.getMessage());
                return null;
            }
        }

        private SnackProduct documentToProduct(Document doc) {
            SnackProduct product = new SnackProduct();
            
            product.setId(doc.getObjectId("_id"));
            product.setProductId(doc.getString("productId"));
            product.setProductName(doc.getString("productName"));
            
            Object priceObj = doc.get("price");
            if (priceObj instanceof Integer) {
                product.setPrice(((Integer) priceObj).doubleValue());
            } else if (priceObj instanceof Double) {
                product.setPrice((Double) priceObj);
            } else {
                product.setPrice(0.0);
            }
            
            Integer stock = doc.getInteger("stock");
            product.setStock(stock != null ? stock : 0);
            
            product.setCategory(doc.getString("category"));
            product.setDescription(doc.getString("description"));
            
            Boolean isActive = doc.getBoolean("isActive");
            product.setActive(isActive != null ? isActive : true);
            
            Date createdAt = doc.getDate("createdAt");
            if (createdAt != null) {
                product.setCreatedAt(createdAt.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            
            Date updatedAt = doc.getDate("updatedAt");
            if (updatedAt != null) {
                product.setUpdatedAt(updatedAt.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            
            return product;
        }

        public boolean saveTransaction(TransactionSnack transaction) {
            try {
                Document doc = new Document()
                    .append("transactionId", transaction.getTransactionId())
                    .append("customerName", transaction.getCustomerName())
                    .append("transactionDate", Date.from(transaction.getTransactionDate()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .append("createdAt", Date.from(transaction.getCreatedAt()
                        .atZone(ZoneId.systemDefault()).toInstant()))
                    .append("items", transaction.getItems())
                    .append("productIds", transaction.getProductIds())
                    .append("productPrices", transaction.getProductPrices())
                    .append("subtotal", transaction.getSubtotal())
                    .append("tax", transaction.getTax())
                    .append("totalPayment", transaction.getTotalPayment())
                    .append("totalItems", transaction.getTotalItems())
                    .append("status", transaction.getStatus())
                    .append("cashGiven", transaction.getCashGiven())
                    .append("change", transaction.getChange())
                    .append("paymentStatus", transaction.getPaymentStatus());

                InsertOneResult result = transactionCollection.insertOne(doc);
                boolean success = result.wasAcknowledged();
                System.out.println("Transaction saved: " + success + " - ID: " + transaction.getTransactionId());
                return success;
            } catch (Exception e) {
                System.err.println("Error saving transaction: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        public List<TransactionSnack> getAllTransactions() {
            List<TransactionSnack> transactions = new ArrayList<>();
            try {
                for (Document doc : transactionCollection.find().sort(new Document("createdAt", -1))) {
                    TransactionSnack transaction = documentToTransaction(doc);
                    transactions.add(transaction);
                }
                System.out.println("Fetched " + transactions.size() + " transactions from database");
            } catch (Exception e) {
                System.err.println("Error fetching transactions: " + e.getMessage());
            }
            return transactions;
        }

        public TransactionSnack getTransactionById(String transactionId) {
            try {
                Document doc = transactionCollection.find(eq("transactionId", transactionId)).first();
                return doc != null ? documentToTransaction(doc) : null;
            } catch (Exception e) {
                System.err.println("Error fetching transaction: " + e.getMessage());
                return null;
            }
        }

        public String getNextTransactionId() {
            try {
                long count = transactionCollection.countDocuments();
                return String.format("SNACK-%03d", count + 1);
            } catch (Exception e) {
                System.err.println("Error generating transaction ID: " + e.getMessage());
                return "SNACK-001";
            }
        }

        private TransactionSnack documentToTransaction(Document doc) {
            TransactionSnack transaction = new TransactionSnack();
            
            transaction.setId(doc.getObjectId("_id"));
            transaction.setTransactionId(doc.getString("transactionId"));
            transaction.setCustomerName(doc.getString("customerName"));
            
            Date transactionDate = doc.getDate("transactionDate");
            if (transactionDate != null) {
                transaction.setTransactionDate(transactionDate.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
            }
            
            Date createdAt = doc.getDate("createdAt");
            if (createdAt != null) {
                transaction.setCreatedAt(createdAt.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            
            Document itemsDoc = (Document) doc.get("items");
            Document productIdsDoc = (Document) doc.get("productIds");
            Document pricesDoc = (Document) doc.get("productPrices");
            
            Map<String, Integer> itemsMap = new HashMap<>();
            if (itemsDoc != null) {
                for (String key : itemsDoc.keySet()) {
                    itemsMap.put(key, itemsDoc.getInteger(key));
                }
            }
            transaction.setItems(itemsMap);
            
            Map<String, String> productIdsMap = new HashMap<>();
            if (productIdsDoc != null) {
                for (String key : productIdsDoc.keySet()) {
                    productIdsMap.put(key, productIdsDoc.getString(key));
                }
            }
            transaction.setProductIds(productIdsMap);
            
            Map<String, Double> pricesMap = new HashMap<>();
            if (pricesDoc != null) {
                for (String key : pricesDoc.keySet()) {
                    Object priceObj = pricesDoc.get(key);
                    if (priceObj instanceof Integer) {
                        pricesMap.put(key, ((Integer) priceObj).doubleValue());
                    } else if (priceObj instanceof Double) {
                        pricesMap.put(key, (Double) priceObj);
                    } else {
                        pricesMap.put(key, 0.0);
                    }
                }
            }
            transaction.setProductPrices(pricesMap);
            
            Object subtotalObj = doc.get("subtotal");
            Object taxObj = doc.get("tax");
            Object totalPaymentObj = doc.get("totalPayment");
            Object cashGivenObj = doc.get("cashGiven");
            Object changeObj = doc.get("change");
            
            transaction.setSubtotal(convertToDouble(subtotalObj));
            transaction.setTax(convertToDouble(taxObj));
            transaction.setTotalPayment(convertToDouble(totalPaymentObj));
            transaction.setCashGiven(convertToDouble(cashGivenObj));
            transaction.setChange(convertToDouble(changeObj));
            
            Integer totalItems = doc.getInteger("totalItems");
            transaction.setTotalItems(totalItems != null ? totalItems : 0);
            
            transaction.setStatus(doc.getString("status"));
            transaction.setPaymentStatus(doc.getString("paymentStatus"));
            
            return transaction;
        }

        private Double convertToDouble(Object obj) {
            if (obj instanceof Integer) {
                return ((Integer) obj).doubleValue();
            } else if (obj instanceof Double) {
                return (Double) obj;
            } else {
                return 0.0;
            }
        }

        public void closeConnection() {
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("MongoDB connection closed");
            }
        }
    }
}