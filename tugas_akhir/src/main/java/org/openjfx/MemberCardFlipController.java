package org.openjfx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.ResourceBundle;

import org.bson.Document;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class MemberCardFlipController implements Initializable {

    @FXML private AnchorPane mainPane;
    @FXML private VBox mainContent;
    @FXML private AnchorPane modernCard;
    @FXML private HBox buttonContainer;
    
    // Card Components - Logo DIHAPUS karena sudah di FXML
    @FXML private ImageView imgFotoDiri;
    @FXML private ImageView imgBarcodeFront;
    @FXML private Label lblMemberIdFront;
    @FXML private Label lblNamaBack;
    @FXML private Label lblTempatLahir;
    @FXML private Label lblJenisKelamin;
    @FXML private Label lblNoHp;
    @FXML private Label lblEmail;
    @FXML private Label lblTanggalDaftar;
    @FXML private Label lblBerlakuHingga;

    private Document memberData;
    private boolean isDataLoaded = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("id", "ID"));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MemberCardController initialized - Modern Single View");
        setupCardStyle();
        // loadLogo() DIHAPUS - logo sudah ada di FXML
    }

    public void setMemberData(Document memberData) {
        this.memberData = memberData;
        if (memberData != null) {
            System.out.println("Member data received: " + memberData.toJson());
            populateCardData();
            generateQRCode();
            loadFotoDiri();
            isDataLoaded = true;
        } else {
            System.out.println("No member data received, using default data");
            setDefaultData();
            generateQRCode();
            createPlaceholderFoto();
        }
    }

    private void setupCardStyle() {
        if (modernCard != null) {
            modernCard.getStyleClass().add("modern-card");
            System.out.println("Modern card style applied");
        }
    }

    private void loadFotoDiri() {
        try {
            System.out.println("Loading foto diri...");
            
            if (memberData != null && memberData.containsKey("foto_diri_base64")) {
                String base64Foto = memberData.getString("foto_diri_base64");
                
                if (base64Foto != null && !base64Foto.trim().isEmpty()) {
                    System.out.println("Base64 foto ditemukan, panjang: " + base64Foto.length());
                    
                    // Clean base64 string
                    String cleanBase64 = base64Foto.replaceAll("data:image/[^;]+;base64,", "").trim();
                    
                    if (!cleanBase64.isEmpty()) {
                        try {
                            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
                            System.out.println("Foto decoded, size: " + imageBytes.length + " bytes");
                            
                            Image foto = new Image(new ByteArrayInputStream(imageBytes));
                            
                            if (!foto.isError()) {
                                imgFotoDiri.setImage(foto);
                                System.out.println("✓ Foto diri berhasil dimuat dari MongoDB");
                                return;
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Invalid base64 format: " + e.getMessage());
                        }
                    }
                }
            }
            
            // Jika tidak ada foto, gunakan placeholder
            System.out.println("Tidak ada foto yang valid, menggunakan placeholder");
            createPlaceholderFoto();
            
        } catch (Exception e) {
            System.out.println("Error loading foto diri: " + e.getMessage());
            createPlaceholderFoto();
        }
    }

    private void createPlaceholderFoto() {
        try {
            WritableImage placeholder = new WritableImage(104, 104);
            PixelWriter pw = placeholder.getPixelWriter();
            
            // Gradient background - KOTAK bukan bulat
            for (int x = 0; x < 104; x++) {
                for (int y = 0; y < 104; y++) {
                    double ratioX = x / 104.0;
                    double ratioY = y / 104.0;
                    Color color = Color.color(
                        0.2 + ratioX * 0.3,
                        0.3 + ratioY * 0.4,
                        0.8 - ratioY * 0.3
                    );
                    pw.setColor(x, y, color);
                }
            }
            
            // Simple user icon - diperbesar
            // Head (lebih besar)
            drawCircle(pw, 52, 35, 18, Color.WHITE);
            
            // Body (lebih besar)
            for (int x = 30; x <= 74; x++) {
                for (int y = 58; y <= 94; y++) {
                    double dx = Math.abs(x - 52);
                    double distanceFromTop = y - 58;
                    // Bentuk trapesium untuk badan
                    if (distanceFromTop > dx * 0.5) {
                        pw.setColor(x, y, Color.WHITE);
                    }
                }
            }
            
            imgFotoDiri.setImage(placeholder);
            System.out.println("✓ Placeholder foto kotak berhasil dibuat");
            
        } catch (Exception e) {
            System.out.println("❌ Gagal membuat placeholder foto: " + e.getMessage());
        }
    }

    private void drawCircle(PixelWriter pw, int centerX, int centerY, int radius, Color color) {
        for (int x = Math.max(0, centerX - radius); x <= Math.min(103, centerX + radius); x++) {
            for (int y = Math.max(0, centerY - radius); y <= Math.min(103, centerY + radius); y++) {
                double dx = x - centerX;
                double dy = y - centerY;
                if (dx * dx + dy * dy <= radius * radius) {
                    pw.setColor(x, y, color);
                }
            }
        }
    }

    private void populateCardData() {
        if (memberData != null) {
            System.out.println("Populating card data from MongoDB...");
            
            try {
                // Member ID
                String memberId = memberData.getString("member_id");
                if (memberId != null && !memberId.trim().isEmpty()) {
                    lblMemberIdFront.setText(memberId.toUpperCase());
                } else {
                    String objectId = memberData.getObjectId("_id").toString();
                    String shortId = objectId.substring(Math.max(0, objectId.length() - 12)).toUpperCase();
                    lblMemberIdFront.setText("MEM-" + shortId);
                }

                // Personal Data
                setLabelText(lblNamaBack, memberData.getString("nama_lengkap"), "NAMA LENGKAP");
                setLabelText(lblTempatLahir, memberData.getString("tempat_lahir"), "Tempat Lahir");
                setLabelText(lblJenisKelamin, memberData.getString("jenis_kelamin"), "Jenis Kelamin");
                setLabelText(lblNoHp, memberData.getString("no_hp"), "No HP");
                setLabelText(lblEmail, memberData.getString("email"), "Email");
                
                // Format tanggal
                Date tanggalDaftar = memberData.getDate("tanggal_daftar");
                Date tanggalBerlakuHingga = memberData.getDate("tanggal_berlaku_hingga");
                
                if (tanggalDaftar != null) {
                    lblTanggalDaftar.setText(dateFormat.format(tanggalDaftar));
                } else {
                    lblTanggalDaftar.setText("-");
                }
                
                if (tanggalBerlakuHingga != null) {
                    lblBerlakuHingga.setText(dateFormat.format(tanggalBerlakuHingga));
                } else {
                    lblBerlakuHingga.setText("-");
                }
                
                System.out.println("✓ Card data populated successfully");
                
            } catch (Exception e) {
                System.out.println("❌ Error populating card data: " + e.getMessage());
                e.printStackTrace();
                setDefaultData();
            }
        } else {
            setDefaultData();
        }
    }

    private void setLabelText(Label label, String value, String fieldName) {
        if (value != null && !value.trim().isEmpty()) {
            label.setText(value.trim());
        } else {
            label.setText("-");
            System.out.println(fieldName + " is empty, using default");
        }
    }

    private void setDefaultData() {
        System.out.println("Setting default data for demo purposes");
        
        lblMemberIdFront.setText("MEM-DEMO-123456");
        lblNamaBack.setText("BAGOES GHENDIS");
        lblTempatLahir.setText("Jombang");
        lblJenisKelamin.setText("Laki-laki");
        lblNoHp.setText("0822341xxxx");
        lblEmail.setText("bagoes@gmail.com");
        lblTanggalDaftar.setText("29 Oktober 2025");
        lblBerlakuHingga.setText("29 Januari 2026");
        
        System.out.println("✓ Default data set successfully");
    }

    private void generateQRCode() {
        try {
            System.out.println("Generating QR code...");
            
            String qrData;
            
            if (memberData != null) {
                qrData = memberData.getString("member_id");
                
                if (qrData == null || qrData.trim().isEmpty()) {
                    String objectId = memberData.getObjectId("_id").toString();
                    qrData = "MEM" + objectId.substring(Math.max(0, objectId.length() - 12)).toUpperCase();
                }
            } else {
                qrData = "DEMO123456789";
            }
            
            System.out.println("QR Code data: " + qrData);
            
            // Generate QR Code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 180, 180);
            
            WritableImage qrImage = new WritableImage(180, 180);
            PixelWriter pixelWriter = qrImage.getPixelWriter();
            
            for (int x = 0; x < 180; x++) {
                for (int y = 0; y < 180; y++) {
                    if (bitMatrix.get(x, y)) {
                        pixelWriter.setColor(x, y, Color.BLACK);
                    } else {
                        pixelWriter.setColor(x, y, Color.WHITE);
                    }
                }
            }
            
            imgBarcodeFront.setImage(qrImage);
            System.out.println("✓ QR Code berhasil digenerate dengan data: " + qrData);
            
        } catch (WriterException e) {
            System.out.println("❌ Gagal generate QR code: " + e.getMessage());
            createPlaceholderQR();
        } catch (Exception e) {
            System.out.println("❌ Error generating QR code: " + e.getMessage());
            e.printStackTrace();
            createPlaceholderQR();
        }
    }

    private void createPlaceholderQR() {
        try {
            WritableImage placeholder = new WritableImage(180, 180);
            PixelWriter pw = placeholder.getPixelWriter();
            
            // White background
            for (int x = 0; x < 180; x++) {
                for (int y = 0; y < 180; y++) {
                    pw.setColor(x, y, Color.WHITE);
                }
            }
            
            // Simple pattern
            for (int i = 0; i < 12; i++) {
                for (int j = 0; j < 12; j++) {
                    if ((i + j) % 2 == 0) {
                        int startX = 10 + i * 13;
                        int startY = 10 + j * 13;
                        for (int x = startX; x < startX + 11 && x < 180; x++) {
                            for (int y = startY; y < startY + 11 && y < 180; y++) {
                                pw.setColor(x, y, Color.BLACK);
                            }
                        }
                    }
                }
            }
            
            imgBarcodeFront.setImage(placeholder);
            System.out.println("✓ Placeholder QR code created");
            
        } catch (Exception e) {
            System.out.println("❌ Gagal membuat placeholder QR: " + e.getMessage());
        }
    }

    @FXML
    private void handlePrint() {
        try {
            System.out.println("Print button clicked");
            
            if (!isDataLoaded && memberData == null) {
                showAlert(Alert.AlertType.WARNING, "Print Warning", 
                    "Tidak ada data member untuk dicetak.\nMenggunakan data demo untuk preview.");
            }
            
            printCard();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Print Error", 
                "Gagal mencetak kartu: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        try {
            System.out.println("Save button clicked");
            
            if (!isDataLoaded && memberData == null) {
                showAlert(Alert.AlertType.WARNING, "Save Warning", 
                    "Tidak ada data member untuk disimpan.\nMenggunakan data demo untuk penyimpanan.");
            }
            
            saveCardAsImage();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Save Error", 
                "Gagal menyimpan kartu: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        System.out.println("Close button clicked");
        try {
            Window window = mainPane.getScene().getWindow();
            if (window != null) {
                window.hide();
                System.out.println("Window closed successfully");
            }
        } catch (Exception e) {
            System.out.println("Error closing window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printCard() {
        try {
            System.out.println("Starting print process...");
            
            boolean wasButtonVisible = buttonContainer.isVisible();
            if (buttonContainer != null) {
                buttonContainer.setVisible(false);
            }
            
            mainPane.applyCss();
            mainPane.layout();
            
            Thread.sleep(100);
            
            javafx.print.PrinterJob printerJob = javafx.print.PrinterJob.createPrinterJob();
            
            if (printerJob != null) {
                boolean showDialog = printerJob.showPrintDialog(mainPane.getScene().getWindow());
                
                if (showDialog) {
                    double scaleX = printerJob.getJobSettings().getPageLayout().getPrintableWidth() / mainPane.getBoundsInParent().getWidth();
                    double scaleY = printerJob.getJobSettings().getPageLayout().getPrintableHeight() / mainPane.getBoundsInParent().getHeight();
                    double scale = Math.min(scaleX, scaleY) * 0.8;
                    
                    mainPane.setScaleX(scale);
                    mainPane.setScaleY(scale);
                    
                    boolean success = printerJob.printPage(mainPane);
                    
                    mainPane.setScaleX(1.0);
                    mainPane.setScaleY(1.0);
                    
                    if (success) {
                        printerJob.endJob();
                        showAlert(Alert.AlertType.INFORMATION, "Print Success", 
                            "Kartu member berhasil dicetak!");
                    }
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Print Error", 
                    "Tidak ada printer yang tersedia.");
            }
            
            if (buttonContainer != null) {
                buttonContainer.setVisible(wasButtonVisible);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Print Error", 
                "Terjadi kesalahan saat mencetak: " + e.getMessage());
        }
    }

    private void saveCardAsImage() {
        try {
            System.out.println("Starting save process...");
            
            boolean wasButtonVisible = buttonContainer.isVisible();
            if (buttonContainer != null) {
                buttonContainer.setVisible(false);
            }
            
            mainPane.applyCss();
            mainPane.layout();
            
            Thread.sleep(200);
            
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            
            WritableImage snapshot = mainPane.snapshot(params, null);
            
            if (snapshot == null) {
                showAlert(Alert.AlertType.ERROR, "Save Error", "Gagal mengambil snapshot kartu.");
                if (buttonContainer != null) buttonContainer.setVisible(wasButtonVisible);
                return;
            }
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Simpan Kartu Member sebagai Gambar");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg")
            );
            
            String fileName = "kartu_member_modern";
            if (memberData != null && memberData.getString("nama_lengkap") != null) {
                String nama = memberData.getString("nama_lengkap")
                    .replaceAll("[^a-zA-Z0-9\\-_]", "_")
                    .toLowerCase();
                fileName = "kartu_member_" + nama;
            }
            fileChooser.setInitialFileName(fileName + ".png");
            
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Downloads"));
            
            Window window = mainPane.getScene().getWindow();
            File file = fileChooser.showSaveDialog(window);
            
            if (file != null) {
                System.out.println("Saving to: " + file.getAbsolutePath());
                java.awt.image.BufferedImage bufferedImage = convertToBufferedImage(snapshot);
                javax.imageio.ImageIO.write(bufferedImage, "png", file);
                showAlert(Alert.AlertType.INFORMATION, "Save Success", 
                    "Kartu member berhasil disimpan di:\n" + file.getAbsolutePath());
            }
            
            if (buttonContainer != null) {
                buttonContainer.setVisible(wasButtonVisible);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Save Error", 
                "Gagal menyimpan file: " + e.getMessage());
            if (buttonContainer != null) {
                buttonContainer.setVisible(true);
            }
        }
    }

    private java.awt.image.BufferedImage convertToBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        
        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB
        );
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = image.getPixelReader().getColor(x, y);
                int argb = convertColorToARGB(color);
                bufferedImage.setRGB(x, y, argb);
            }
        }
        
        return bufferedImage;
    }

    private int convertColorToARGB(Color color) {
        int a = (int) Math.round(color.getOpacity() * 255);
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        try {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStyleClass().add("custom-alert");
            
            alert.showAndWait();
        } catch (Exception e) {
            System.out.println("Error showing alert: " + e.getMessage());
            System.out.println("ALERT: " + title + " - " + message);
        }
    }

    public void refreshData() {
        if (memberData != null) {
            populateCardData();
            generateQRCode();
            loadFotoDiri();
        }
    }
    
    public Document getMemberData() {
        return memberData;
    }
    
    public void updateMemberData(Document newData) {
        this.memberData = newData;
        refreshData();
    }
}