package org.openjfx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

// Import untuk webcam
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

public class GymRegistrationController implements Initializable {

    private static final Logger logger = Logger.getLogger(GymRegistrationController.class.getName());
    
    // Komponen UI dari FXML
    @FXML private TextField txtNama;
    @FXML private TextField txtTempatLahir;
    @FXML private DatePicker dateTglLahir;
    @FXML private ComboBox<String> comboJenisKelamin;
    @FXML private TextField txtNIK;
    @FXML private ComboBox<String> comboPekerjaan;
    @FXML private ComboBox<String> comboDurasiMember;
    @FXML private TextArea txtAlamat;
    @FXML private TextField txtNoHp;
    @FXML private TextField txtEmail;
    @FXML private ImageView imgFotoDiri;
    @FXML private Button btnPilihFoto;
    @FXML private Button btnDaftar;
    @FXML private Label jam;
    @FXML private ImageView imgkembali;
    
    // Variabel
    private StringProperty currentTime = new SimpleStringProperty();
    private MongoCollection<Document> membersCollection;
    private boolean isWebcamActive = false;
    private File selectedImageFile;
    private byte[] imageBytes;
    private Random random = new Random();
    private Webcam webcam;
    private int currentAge = 0;
    private Timeline webcamTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setupDatabase();
            setupUI();
            startClock();
            logger.info("Controller initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing controller", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error", 
                "Gagal menginisialisasi aplikasi: " + e.getMessage());
        }
    }
    
    private void setupDatabase() {
        try {
            String connectionString = "mongodb://localhost:27017";
            MongoClient mongoClient = MongoClients.create(connectionString);
            MongoDatabase database = mongoClient.getDatabase("gym");
            membersCollection = database.getCollection("data_members");
            logger.info("Connected to MongoDB successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                "Failed to connect to MongoDB: " + e.getMessage());
        }
    }
    
    private void setupUI() {
        try {
            // Setup ComboBox Jenis Kelamin
            ObservableList<String> jenisKelaminOptions = FXCollections.observableArrayList(
                "Laki-laki", "Perempuan"
            );
            comboJenisKelamin.setItems(jenisKelaminOptions);
            comboJenisKelamin.setPromptText("-- Pilih Jenis Kelamin --");
            
            // Setup ComboBox Pekerjaan/Status
            ObservableList<String> pekerjaanOptions = FXCollections.observableArrayList(
                "Pelajar",
                "PNS",
                "TNI",
                "POLRI",
                "Pelaku Budaya / Ulama",
                "Guru"
            );
            comboPekerjaan.setItems(pekerjaanOptions);
            comboPekerjaan.setPromptText("-- Pilih Pekerjaan / Status --");
            
            // Setup DatePicker dengan konfigurasi yang benar
            setupDatePicker();
            
            // Setup TextFormatter untuk NIK - hanya angka, max 16 digit
            txtNIK.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    txtNIK.setText(newValue.replaceAll("[^\\d]", ""));
                }
                if (newValue.length() > 16) {
                    txtNIK.setText(newValue.substring(0, 16));
                }
            });
            
            // Setup TextFormatter untuk No HP - hanya angka dan karakter telepon
            txtNoHp.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("[0-9+\\-\\s()]*")) {
                    txtNoHp.setText(oldValue);
                }
            });
            
            // Setup event handlers
            btnPilihFoto.setOnAction(this::handlePilihFoto);
            btnDaftar.setOnAction(this::handleDaftar);
            
            // Setup kembali button
            if (imgkembali != null) {
                imgkembali.setOnMouseClicked(event -> handleKembali());
                imgkembali.setStyle("-fx-cursor: hand;");
            }
            
            // Set placeholder untuk foto
            setPlaceholderImages();
            
            // Setup enter key untuk form
            setupEnterKeyHandler();
            
            logger.info("UI setup completed");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up UI", e);
        }
    }
    
    /**
     * Setup DatePicker dengan konfigurasi yang benar
     */
    private void setupDatePicker() {
        try {
            // Set prompt text
            dateTglLahir.setPromptText("DD/MM/YYYY");
            
            // Buat custom StringConverter
            dateTglLahir.setConverter(new StringConverter<LocalDate>() {
                private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                
                @Override
                public String toString(LocalDate date) {
                    if (date != null) {
                        return dateFormatter.format(date);
                    }
                    return "";
                }
                
                @Override
                public LocalDate fromString(String string) {
                    if (string != null && !string.trim().isEmpty()) {
                        try {
                            String cleanedString = string.trim().replaceAll("[^0-9/]", "");
                            return LocalDate.parse(cleanedString, dateFormatter);
                        } catch (DateTimeParseException e) {
                            logger.warning("Invalid date format: '" + string + "'");
                            return null;
                        }
                    }
                    return null;
                }
            });
            
            // Set day cell factory untuk validasi
            dateTglLahir.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (item != null && !empty) {
                        // Disable tanggal di masa depan
                        if (item.isAfter(LocalDate.now())) {
                            setDisable(true);
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #ccc;");
                        }
                        // Disable tanggal terlalu lama (lebih dari 100 tahun)
                        else if (item.isBefore(LocalDate.now().minusYears(100))) {
                            setDisable(true);
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #ccc;");
                        }
                    }
                }
            });
            
            // Listener untuk menangani perubahan tanggal
            dateTglLahir.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    try {
                        // Update paket member berdasarkan umur
                        updateMemberPackages(newValue);
                        
                        logger.info("Tanggal lahir dipilih: " + 
                            newValue.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        
                        dateTglLahir.setValue(newValue);
                        
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error handling date selection", e);
                    }
                }
            });
            
            // Event handler untuk onAction
            dateTglLahir.setOnAction(event -> {
                LocalDate selectedDate = dateTglLahir.getValue();
                if (selectedDate != null) {
                    logger.info("DatePicker onAction triggered: " + 
                        selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    Platform.runLater(() -> comboJenisKelamin.requestFocus());
                }
            });
            
            logger.info("DatePicker setup completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up DatePicker", e);
            showAlert(Alert.AlertType.ERROR, "DatePicker Error", 
                "Gagal mengatur DatePicker: " + e.getMessage());
        }
    }
    
    /**
     * Update paket member berdasarkan umur
     */
    private void updateMemberPackages(LocalDate birthDate) {
        try {
            if (birthDate == null) {
                logger.warning("BirthDate is null in updateMemberPackages");
                return;
            }
            
            LocalDate today = LocalDate.now();
            Period period = Period.between(birthDate, today);
            currentAge = period.getYears();
            
            ObservableList<String> durasiOptions = FXCollections.observableArrayList();
            
            if (currentAge < 15) {
                // Di bawah 15 tahun - WAJIB dengan pelatih
                durasiOptions.addAll(
                    "1 Bulan + Pelatih",
                    "2 Bulan + Pelatih",
                    "3 Bulan + Pelatih",
                    "6 Bulan + Pelatih",
                    "1 Tahun + Pelatih"
                );
            } else {
                // 15 tahun ke atas - bisa pilih dengan atau tanpa pelatih
                durasiOptions.addAll(
                    "1 Bulan",
                    "1 Bulan + Pelatih",
                    "2 Bulan",
                    "2 Bulan + Pelatih",
                    "3 Bulan",
                    "3 Bulan + Pelatih",
                    "6 Bulan",
                    "6 Bulan + Pelatih",
                    "1 Tahun",
                    "1 Tahun + Pelatih"
                );
            }
            
            // Update ComboBox items
            comboDurasiMember.setItems(durasiOptions);
            comboDurasiMember.setPromptText("-- Pilih Durasi Member --");
            
            // Set default value
            if (!durasiOptions.isEmpty()) {
                if (currentAge < 15) {
                    comboDurasiMember.setValue("1 Bulan + Pelatih");
                } else {
                    comboDurasiMember.setValue("1 Bulan");
                }
            }
            
            logger.info("Paket member diupdate untuk umur: " + currentAge + " tahun");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating member packages", e);
        }
    }
    
    private void setupEnterKeyHandler() {
        // Enter key untuk berpindah field
        txtNama.setOnAction(e -> txtTempatLahir.requestFocus());
        txtTempatLahir.setOnAction(e -> dateTglLahir.requestFocus());
        
        dateTglLahir.setOnAction(e -> {
            if (dateTglLahir.getValue() != null) {
                comboJenisKelamin.requestFocus();
            }
        });
        
        comboJenisKelamin.setOnAction(e -> txtNIK.requestFocus());
        txtNIK.setOnAction(e -> comboPekerjaan.requestFocus());
        comboPekerjaan.setOnAction(e -> comboDurasiMember.requestFocus());
        comboDurasiMember.setOnAction(e -> txtAlamat.requestFocus());
        txtAlamat.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                txtNoHp.requestFocus();
            }
        });
        txtNoHp.setOnAction(e -> txtEmail.requestFocus());
        txtEmail.setOnAction(e -> handleDaftar(null));
    }
    
    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            jam.setText(format.format(new Date()));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
    
    private void setPlaceholderImages() {
        try {
            InputStream placeholderStream = getClass().getResourceAsStream("/images/placeholder.png");
            if (placeholderStream != null) {
                Image placeholder = new Image(placeholderStream);
                imgFotoDiri.setImage(placeholder);
            } else {
                imgFotoDiri.setImage(createDefaultPlaceholder());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Placeholder image not found, using default");
            imgFotoDiri.setImage(createDefaultPlaceholder());
        }
    }
    
    private Image createDefaultPlaceholder() {
        WritableImage image = new WritableImage(250, 250);
        PixelWriter pixelWriter = image.getPixelWriter();
        
        for (int y = 0; y < 250; y++) {
            for (int x = 0; x < 250; x++) {
                pixelWriter.setColor(x, y, Color.LIGHTGRAY);
            }
        }
        
        return image;
    }
    
    @FXML
    private void handlePilihFoto(ActionEvent event) {
        try {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Webcam", "Webcam", "File dari Komputer");
            dialog.setTitle("Pilih Sumber Foto");
            dialog.setHeaderText("Pilih sumber untuk mengambil foto");
            dialog.setContentText("Pilih metode:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if ("Webcam".equals(result.get())) {
                    handleWebcamFoto();
                } else {
                    handleFileFoto();
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in handlePilihFoto", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memproses pilihan foto: " + e.getMessage());
        }
    }
    
    /**
     * Convert BufferedImage ke JavaFX Image
     */
    private Image convertBufferedImageToFXImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        
        try {
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            
            WritableImage writableImage = new WritableImage(width, height);
            PixelWriter pixelWriter = writableImage.getPixelWriter();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = bufferedImage.getRGB(x, y);
                    
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    
                    Color color = Color.rgb(r, g, b, a / 255.0);
                    pixelWriter.setColor(x, y, color);
                }
            }
            
            return writableImage;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error converting BufferedImage to FX Image", e);
            return null;
        }
    }
    
    /**
     * Convert BufferedImage ke byte array
     */
    private byte[] convertBufferedImageToBytes(BufferedImage bufferedImage) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error converting BufferedImage to bytes", e);
            return null;
        }
    }
    
    /**
     * Handle pengambilan foto melalui webcam
     */
    private void handleWebcamFoto() {
        try {
            if (Webcam.getWebcams().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Webcam Tidak Ditemukan", 
                    "Tidak ada webcam yang terdeteksi. Silakan pilih foto dari file.");
                handleFileFoto();
                return;
            }
            
            webcam = Webcam.getDefault();
            if (webcam == null) {
                showAlert(Alert.AlertType.ERROR, "Webcam Error", 
                    "Gagal mengakses webcam. Silakan pilih foto dari file.");
                handleFileFoto();
                return;
            }
            
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            
            if (!webcam.isOpen()) {
                webcam.open();
            }
            
            Stage webcamStage = new Stage();
            webcamStage.setTitle("Webcam Preview - Ambil Foto");
            webcamStage.setResizable(false);
            
            VBox root = new VBox(10);
            root.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #f5f5f5;");
            
            Label instructionLabel = new Label("Posisikan diri Anda dan klik 'Ambil Foto'");
            instructionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
            
            ImageView previewImageView = new ImageView();
            previewImageView.setFitHeight(400);
            previewImageView.setFitWidth(400);
            previewImageView.setPreserveRatio(true);
            previewImageView.setStyle("-fx-border-color: #ccc; -fx-border-width: 2px; -fx-background-color: white;");
            
            Label statusLabel = new Label("Webcam aktif - Preview real-time");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4CAF50;");
            
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            
            Button captureButton = new Button("Ambil Foto");
            captureButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-color: #4CAF50; -fx-text-fill: white;");
            
            Button cancelButton = new Button("Batal");
            cancelButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-color: #f44336; -fx-text-fill: white;");
            
            buttonBox.getChildren().addAll(captureButton, cancelButton);
            root.getChildren().addAll(instructionLabel, previewImageView, statusLabel, buttonBox);
            
            Scene scene = new Scene(root, 500, 550);
            webcamStage.setScene(scene);
            
            webcamTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
                if (webcam != null && webcam.isOpen()) {
                    BufferedImage bufferedImage = webcam.getImage();
                    if (bufferedImage != null) {
                        Image fxImage = convertBufferedImageToFXImage(bufferedImage);
                        if (fxImage != null) {
                            previewImageView.setImage(fxImage);
                        }
                    }
                }
            }));
            webcamTimeline.setCycleCount(Animation.INDEFINITE);
            webcamTimeline.play();
            
            captureButton.setOnAction(e -> {
                if (webcam != null && webcam.isOpen()) {
                    BufferedImage bufferedImage = webcam.getImage();
                    if (bufferedImage != null) {
                        try {
                            imageBytes = convertBufferedImageToBytes(bufferedImage);
                            
                            Image fxImage = convertBufferedImageToFXImage(bufferedImage);
                            if (fxImage != null) {
                                imgFotoDiri.setImage(fxImage);
                                
                                showAlert(Alert.AlertType.INFORMATION, "Sukses", 
                                    "Foto berhasil diambil dari webcam.");
                                logger.info("Foto berhasil diambil dari webcam");
                                
                                webcamStage.close();
                                stopWebcam();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Error", 
                                    "Gagal mengkonversi gambar dari webcam.");
                            }
                            
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Error capturing webcam image", ex);
                            showAlert(Alert.AlertType.ERROR, "Error", 
                                "Gagal mengambil foto: " + ex.getMessage());
                        }
                    }
                }
            });
            
            cancelButton.setOnAction(e -> {
                webcamStage.close();
                stopWebcam();
            });
            
            webcamStage.setOnCloseRequest(evt -> {
                stopWebcam();
            });
            
            webcamStage.showAndWait();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in handleWebcamFoto", e);
            showAlert(Alert.AlertType.ERROR, "Webcam Error", 
                "Gagal mengakses webcam: " + e.getMessage());
            
            stopWebcam();
            handleFileFoto();
        }
    }
    
    /**
     * Stop webcam dan cleanup resources
     */
    private void stopWebcam() {
        try {
            if (webcamTimeline != null) {
                webcamTimeline.stop();
                webcamTimeline = null;
            }
            
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
                webcam = null;
                logger.info("Webcam stopped successfully");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error stopping webcam", e);
        }
    }
    
    /**
     * Handle pemilihan foto dari file
     */
    private void handleFileFoto() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Pilih Foto Diri");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );
            
            File file = fileChooser.showOpenDialog(btnPilihFoto.getScene().getWindow());
            if (file != null) {
                if (file.length() > 5 * 1024 * 1024) {
                    showAlert(Alert.AlertType.ERROR, "File Terlalu Besar", 
                        "File terlalu besar. Maksimal 5MB.");
                    return;
                }
                
                Image image = loadImageFromFile(file);
                if (image != null) {
                    imgFotoDiri.setImage(image);
                    selectedImageFile = file;
                    imageBytes = convertImageToBytes(file);
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", 
                        "Foto berhasil dipilih dari file.");
                    logger.info("Foto dipilih dari file: " + file.getAbsolutePath());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                        "Gagal memuat foto dari file.");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading image from file", e);
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Gagal memuat foto: " + e.getMessage());
        }
    }
    
    private Image loadImageFromFile(File file) {
        try {
            String imagePath = file.toURI().toString();
            return new Image(imagePath, 250, 250, true, true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating image from file", e);
            return null;
        }
    }
    
    private byte[] convertImageToBytes(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error converting image to bytes", e);
            return null;
        }
    }
    
    /**
     * Generate ID VIP baru
     */
    private String generateVIPId() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
            String timestamp = dateFormat.format(new Date());
            int randomNum = random.nextInt(10000);
            return String.format("VIP-%s-%04d", timestamp, randomNum);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating VIP ID", e);
            return "VIP-" + System.currentTimeMillis();
        }
    }
    
    @FXML
    private void handleDaftar(ActionEvent event) {
        try {
            logger.info("Memproses pendaftaran VIP...");
            
            if (!validateForm()) {
                return;
            }
            
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Konfirmasi Pendaftaran VIP");
            confirmation.setHeaderText("Simpan Data Member VIP?");
            confirmation.setContentText("Apakah Anda yakin ingin menyimpan data member VIP ini?");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                saveMemberToDatabase();
            } else {
                logger.info("Pendaftaran VIP dibatalkan oleh user");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in handleDaftar", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Terjadi kesalahan: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        if (txtNama.getText().trim().isEmpty()) {
            errors.append("• Nama lengkap harus diisi\n");
        } else if (txtNama.getText().trim().length() < 3) {
            errors.append("• Nama lengkap minimal 3 karakter\n");
        }
        
        if (txtTempatLahir.getText().trim().isEmpty()) {
            errors.append("• Tempat lahir harus diisi\n");
        }
        
        if (dateTglLahir.getValue() == null) {
            errors.append("• Tanggal lahir harus diisi\n");
        } else {
            LocalDate selectedDate = dateTglLahir.getValue();
            LocalDate today = LocalDate.now();
            
            if (selectedDate.isAfter(today.minusYears(5))) {
                errors.append("• Umur minimal 5 tahun\n");
            }
            
            if (selectedDate.isBefore(today.minusYears(100))) {
                errors.append("• Tanggal lahir tidak valid (terlalu lama)\n");
            }
            
            if (selectedDate.isAfter(today)) {
                errors.append("• Tanggal lahir tidak boleh di masa depan\n");
            }
        }
        
        if (comboJenisKelamin.getValue() == null) {
            errors.append("• Jenis kelamin harus dipilih\n");
        }
        
        if (txtNIK.getText().trim().isEmpty()) {
            errors.append("• NIK harus diisi\n");
        } else if (!txtNIK.getText().trim().matches("\\d{16}")) {
            errors.append("• NIK harus 16 digit angka\n");
        }
        
        // Validasi Pekerjaan/Status
        if (comboPekerjaan.getValue() == null) {
            errors.append("• Pekerjaan / Status harus dipilih\n");
        }
        
        if (comboDurasiMember.getValue() == null) {
            errors.append("• Durasi member harus dipilih\n");
        }
        
        if (txtAlamat.getText().trim().isEmpty()) {
            errors.append("• Alamat domisili harus diisi\n");
        } else if (txtAlamat.getText().trim().length() < 10) {
            errors.append("• Alamat terlalu pendek, minimal 10 karakter\n");
        }
        
        if (txtNoHp.getText().trim().isEmpty()) {
            errors.append("• No. HP/WhatsApp harus diisi\n");
        } else if (!txtNoHp.getText().trim().matches("^[0-9+\\-\\s()]{10,}$")) {
            errors.append("• Format nomor HP tidak valid (minimal 10 digit)\n");
        }
        
        if (txtEmail.getText().trim().isEmpty()) {
            errors.append("• Email harus diisi\n");
        } else if (!isValidEmail(txtEmail.getText().trim())) {
            errors.append("• Format email tidak valid\n");
        }
        
        if (imageBytes == null || imageBytes.length == 0) {
            errors.append("• Foto diri harus diambil\n");
        }
        
        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Validasi Error", 
                "Silakan perbaiki kesalahan berikut:\n\n" + errors.toString());
            return false;
        }
        
        return true;
    }
    
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    private void saveMemberToDatabase() {
        btnDaftar.setDisable(true);
        btnDaftar.setText("Menyimpan VIP...");
        
        new Thread(() -> {
            try {
                String vipId = generateVIPId();
                
                Document memberDoc = new Document();
                memberDoc.append("member_id", vipId);
                memberDoc.append("member_type", "VIP");
                memberDoc.append("nama_lengkap", txtNama.getText().trim());
                memberDoc.append("tempat_lahir", txtTempatLahir.getText().trim());
                
                LocalDate tanggalLahir = dateTglLahir.getValue();
                if (tanggalLahir != null) {
                    memberDoc.append("tanggal_lahir", tanggalLahir.format(DateTimeFormatter.ISO_DATE));
                    memberDoc.append("tanggal_lahir_display", tanggalLahir.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    logger.info("Menyimpan tanggal lahir: " + tanggalLahir);
                } else {
                    throw new IllegalArgumentException("Tanggal lahir tidak boleh null");
                }
                
                memberDoc.append("umur", currentAge);
                memberDoc.append("jenis_kelamin", comboJenisKelamin.getValue());
                memberDoc.append("nik", txtNIK.getText().trim());
                
                // Simpan pekerjaan/status
                String pekerjaan = comboPekerjaan.getValue();
                memberDoc.append("pekerjaan", pekerjaan);
                
                // Simpan durasi member
                String durasiMember = comboDurasiMember.getValue();
                memberDoc.append("durasi_member", durasiMember);
                memberDoc.append("durasi_bulan", getDurasiMemberBulan(durasiMember));
                memberDoc.append("dengan_pelatih", durasiMember.contains("+ Pelatih"));
                
                // Hitung tanggal berlaku hingga
                Date tanggalBerlakuHingga = getTanggalBerlakuHingga(durasiMember);
                memberDoc.append("tanggal_berlaku_hingga", tanggalBerlakuHingga);
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                memberDoc.append("tanggal_berlaku_hingga_str", dateFormat.format(tanggalBerlakuHingga));
                
                memberDoc.append("alamat_domisili", txtAlamat.getText().trim());
                memberDoc.append("no_hp", txtNoHp.getText().trim());
                memberDoc.append("email", txtEmail.getText().trim());
                
                Date tanggalDaftar = new Date();
                memberDoc.append("tanggal_daftar", tanggalDaftar);
                memberDoc.append("tanggal_daftar_str", dateFormat.format(tanggalDaftar));
                
                SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                memberDoc.append("waktu_pendaftaran", timeFormat.format(tanggalDaftar));
                
                // Simpan foto diri sebagai Base64
                if (imageBytes != null && imageBytes.length > 0) {
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    memberDoc.append("foto_diri_base64", base64Image);
                    
                    if (selectedImageFile != null) {
                        memberDoc.append("foto_diri_source", "file");
                        memberDoc.append("foto_diri_path", selectedImageFile.getAbsolutePath());
                    } else {
                        memberDoc.append("foto_diri_source", "webcam");
                    }
                    
                    memberDoc.append("foto_diri_timestamp", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                    memberDoc.append("foto_diri_size", imageBytes.length);
                }
                
                memberDoc.append("status_keanggotaan", "VIP Aktif");
                memberDoc.append("created_at", new Date());
                
                // Insert ke MongoDB
                InsertOneResult result = membersCollection.insertOne(memberDoc);
                
                Platform.runLater(() -> {
                    if (result.getInsertedId() != null) {
                        String mongoId = result.getInsertedId().asObjectId().getValue().toString();
                        logger.info("Data VIP berhasil disimpan dengan ID: " + vipId);
                        logger.info("MongoDB ID: " + mongoId);
                        logger.info("Pekerjaan: " + pekerjaan);
                        
                        Document savedMember = membersCollection.find(
                            new Document("_id", result.getInsertedId())).first();
                        
                        showSuccessAndReset(vipId, savedMember);
                    } else {
                        logger.severe("Gagal menyimpan data - InsertedId is null");
                        showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan data ke database");
                        resetButtonState();
                    }
                });
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error saving VIP member to database", e);
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Database Error", 
                        "Error saving to MongoDB: " + e.getMessage());
                    resetButtonState();
                });
            }
        }).start();
    }
    
    /**
     * Mendapatkan durasi member dalam bulan
     */
    private int getDurasiMemberBulan(String durasiMember) {
        if (durasiMember != null) {
            if (durasiMember.contains("1 Bulan")) return 1;
            if (durasiMember.contains("2 Bulan")) return 2;
            if (durasiMember.contains("3 Bulan")) return 3;
            if (durasiMember.contains("6 Bulan")) return 6;
            if (durasiMember.contains("1 Tahun")) return 12;
        }
        return 1;
    }
    
    /**
     * Menghitung tanggal berlaku hingga
     */
    private Date getTanggalBerlakuHingga(String durasiMember) {
        int durasiBulan = getDurasiMemberBulan(durasiMember);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, durasiBulan);
        return cal.getTime();
    }
    
    private void showSuccessAndReset(String vipId, Document savedMember) {
        try {
            String denganPelatih = savedMember.getBoolean("dengan_pelatih", false) ? 
                "✓ Dengan Pelatih Pribadi" : "× Tanpa Pelatih";
            
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Sukses Pendaftaran VIP");
            successAlert.setHeaderText("🎉 PENDAFTARAN VIP BERHASIL 🎉");
            successAlert.setContentText(
                "Selamat! Anda telah terdaftar sebagai member VIP.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "ID VIP: " + vipId + "\n" +
                "Nama: " + savedMember.getString("nama_lengkap") + "\n" +
                "Umur: " + savedMember.getInteger("umur") + " tahun\n" +
                "Pekerjaan: " + savedMember.getString("pekerjaan") + "\n" +
                "Durasi: " + savedMember.getString("durasi_member") + "\n" +
                denganPelatih + "\n" +
                "Status: VIP Aktif\n" +
                "Berlaku hingga: " + savedMember.getString("tanggal_berlaku_hingga_str") + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Klik OK untuk melihat kartu member Anda."
            );
            
            Optional<ButtonType> result = successAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                showMemberCard(savedMember);
            }
            
            resetForm();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in showSuccessAndReset", e);
            resetForm();
        }
    }
    
    /**
     * Method untuk membuka kartu member
     */
    private void showMemberCard(Document memberData) {
        try {
            logger.info("Membuka kartu member untuk: " + memberData.getString("nama_lengkap"));
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/kartu/membercard.fxml"));
            Parent root = loader.load();
            
            MemberCardFlipController cardController = loader.getController();
            cardController.setMemberData(memberData);
            
            Stage cardStage = new Stage();
            cardStage.setTitle("Kartu Member - " + memberData.getString("nama_lengkap"));
            cardStage.setScene(new Scene(root));
            cardStage.setMaximized(true);
            cardStage.show();
            
            logger.info("Kartu member berhasil ditampilkan");
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gagal memuat kartu member", e);
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Gagal menampilkan kartu member: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error showing member card", e);
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Terjadi kesalahan saat menampilkan kartu member: " + e.getMessage());
        }
    }
    
    private void resetForm() {
        Platform.runLater(() -> {
            try {
                txtNama.clear();
                txtTempatLahir.clear();
                dateTglLahir.setValue(null);
                comboJenisKelamin.getSelectionModel().clearSelection();
                txtNIK.clear();
                comboPekerjaan.getSelectionModel().clearSelection();
                comboDurasiMember.setValue(null);
                txtAlamat.clear();
                txtNoHp.clear();
                txtEmail.clear();
                
                selectedImageFile = null;
                imageBytes = null;
                setPlaceholderImages();
                
                currentAge = 0;
                
                resetButtonState();
                
                txtNama.requestFocus();
                
                logger.info("Form berhasil direset");
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error resetting form", e);
            }
        });
    }
    
    private void resetButtonState() {
        btnDaftar.setDisable(false);
        btnDaftar.setText("DAFTAR VIP");
    }
    
    private void handleKembali() {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Konfirmasi");
            confirmation.setHeaderText("Kembali ke Menu Utama?");
            confirmation.setContentText("Data yang belum disimpan akan hilang.");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                stopWebcam();
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_member/menu_member.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) imgkembali.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gagal kembali ke menu utama", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Gagal memuat halaman menu utama");
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Method untuk cleanup resources
     */
    public void cleanup() {
        try {
            stopWebcam();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during cleanup", e);
        }
    }
    
    /**
     * Method untuk set stage
     */
    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            cleanup();
        });
    }
    
    /**
     * Getter methods
     */
    public ComboBox<String> getComboDurasiMember() {
        return comboDurasiMember;
    }
    
    public ComboBox<String> getComboPekerjaan() {
        return comboPekerjaan;
    }
    
    public int getCurrentAge() {
        return currentAge;
    }
}