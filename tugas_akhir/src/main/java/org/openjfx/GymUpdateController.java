package org.openjfx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import static com.mongodb.client.model.Filters.*;

public class GymUpdateController implements Initializable {

    @FXML private Label jam;
    @FXML private ImageView imgkembali;
    @FXML private TextField txtCariMember;
    @FXML private Button btnCari;
    @FXML private Label lblStatusCari;
    
    @FXML private TextField txtIdMember;
    @FXML private TextField txtNama;
    @FXML private TextField txtTempatLahir;
    @FXML private DatePicker dateTglLahir;
    @FXML private ComboBox<String> comboJenisKelamin;
    @FXML private TextField txtNIK;
    @FXML private ComboBox<String> comboPekerjaan; // FIELD BARU
    @FXML private TextArea txtAlamat;
    @FXML private TextField txtNoHp;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> comboDurasiMember;
    
    @FXML private ImageView imgFotoDiri;
    @FXML private Button btnPilihFoto;
    @FXML private Label lblDurasiMember;
    @FXML private Label lblStatusMember;
    @FXML private Label lblMasaBerlaku;
    @FXML private ComboBox<String> comboPerpanjang;
    @FXML private Button btnPerpanjang;
    
    @FXML private Button btnUpdate;
    @FXML private Button btnReset;
    @FXML private Label lblLastUpdate;
    @FXML private Label lblAdmin;

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> membersCollection;
    private String currentPhotoPath;
    private ObjectId currentMemberId;
    private LocalDate masaAktifHingga;
    private int currentAge = 0;
    private String originalFotoBase64;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setupMongoDBConnection();
            initializeClock();
            setupEventHandlers();
            initializeComboBoxes();
            setupInitialState();
            setupDatePicker();
            System.out.println("GymUpdateController initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing GymUpdateController: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Initialization Error", 
                     "Gagal menginisialisasi controller: " + e.getMessage());
        }
    }

    private void setupMongoDBConnection() {
        try {
            String connectionString = "mongodb://localhost:27017";
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase("gym");
            membersCollection = database.getCollection("data_members");
            
            System.out.println("Connected to MongoDB successfully");
            
        } catch (Exception e) {
            System.err.println("Database connection error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Tidak dapat terhubung ke MongoDB: " + e.getMessage());
        }
    }

    private void initializeClock() {
        try {
            Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
                DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss");
                jam.setText(LocalDateTime.now().format(format));
            }), new KeyFrame(Duration.seconds(1)));
            clock.setCycleCount(Animation.INDEFINITE);
            clock.play();
        } catch (Exception e) {
            System.err.println("Error initializing clock: " + e.getMessage());
        }
    }

    private void setupDatePicker() {
        try {
            dateTglLahir.setPromptText("DD/MM/YYYY");
            
            dateTglLahir.setConverter(new javafx.util.StringConverter<LocalDate>() {
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
                        } catch (Exception e) {
                            System.err.println("Invalid date format: '" + string + "'");
                            return null;
                        }
                    }
                    return null;
                }
            });
            
            dateTglLahir.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (item != null && !empty) {
                        if (item.isAfter(LocalDate.now())) {
                            setDisable(true);
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #ccc;");
                        }
                        else if (item.isBefore(LocalDate.now().minusYears(100))) {
                            setDisable(true);
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #ccc;");
                        }
                    }
                }
            });
            
            dateTglLahir.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    updateMemberPackages(newValue);
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error setting up DatePicker: " + e.getMessage());
        }
    }

    private void updateMemberPackages(LocalDate birthDate) {
        try {
            if (birthDate == null) return;
            
            LocalDate today = LocalDate.now();
            java.time.Period period = java.time.Period.between(birthDate, today);
            currentAge = period.getYears();
            
            if (comboDurasiMember != null) {
                List<String> durasiOptions = new ArrayList<>();
                
                if (currentAge < 15) {
                    durasiOptions.addAll(Arrays.asList(
                        "1 Bulan + Pelatih",
                        "2 Bulan + Pelatih", 
                        "3 Bulan + Pelatih",
                        "6 Bulan + Pelatih",
                        "1 Tahun + Pelatih"
                    ));
                } else {
                    durasiOptions.addAll(Arrays.asList(
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
                    ));
                }
                
                comboDurasiMember.getItems().setAll(durasiOptions);
                
                if (comboDurasiMember.getValue() == null) {
                    if (currentAge < 15) {
                        comboDurasiMember.setValue("1 Bulan + Pelatih");
                    } else {
                        comboDurasiMember.setValue("1 Bulan");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error updating member packages: " + e.getMessage());
        }
    }

    private void setupEventHandlers() {
        try {
            if (imgkembali != null) {
                imgkembali.setOnMouseClicked(event -> {
                    try {
                        kembaliKeMenuMember();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Navigation Error", 
                                 "Tidak dapat kembali ke menu member: " + e.getMessage());
                    }
                });
            }

            if (btnCari != null) {
                btnCari.setOnAction(event -> searchMember());
            }
            
            if (txtCariMember != null) {
                txtCariMember.setOnAction(event -> searchMember());
            }

            if (btnPilihFoto != null) {
                btnPilihFoto.setOnAction(event -> selectPhoto());
            }

            if (btnUpdate != null) {
                btnUpdate.setOnAction(event -> updateMemberData());
            }

            if (btnPerpanjang != null) {
                btnPerpanjang.setOnAction(event -> perpanjangMember());
            }

            if (btnReset != null) {
                btnReset.setOnAction(event -> resetForm());
            }

            if (txtNIK != null) {
                txtNIK.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("\\d*")) {
                        txtNIK.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                    if (newValue.length() > 16) {
                        txtNIK.setText(newValue.substring(0, 16));
                    }
                });
            }

            if (txtNoHp != null) {
                txtNoHp.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("[0-9+\\-\\s()]*")) {
                        txtNoHp.setText(oldValue);
                    }
                });
            }

        } catch (Exception e) {
            System.err.println("Error setting up event handlers: " + e.getMessage());
        }
    }

    private void initializeComboBoxes() {
        try {
            // Jenis Kelamin
            if (comboJenisKelamin != null) {
                comboJenisKelamin.getItems().clear();
                comboJenisKelamin.getItems().addAll("Laki-laki", "Perempuan");
            }
            
            // Pekerjaan - COMBO BOX BARU
            if (comboPekerjaan != null) {
                comboPekerjaan.getItems().clear();
                comboPekerjaan.getItems().addAll(
                    "Pelajar",
                    "PNS",
                    "TNI",
                    "Polri",
                    "Pelaku Budaya",
                    "Ulama",
                    "Guru"
                );
                comboPekerjaan.setPromptText("Pilih Pekerjaan");
            }
            
            // Perpanjangan
            if (comboPerpanjang != null) {
                comboPerpanjang.getItems().clear();
                comboPerpanjang.getItems().addAll(
                    "1 Bulan",
                    "3 Bulan",
                    "6 Bulan", 
                    "1 Tahun"
                );
            }

            // Durasi Member
            if (comboDurasiMember != null) {
                comboDurasiMember.getItems().clear();
            }
        } catch (Exception e) {
            System.err.println("Error initializing combo boxes: " + e.getMessage());
        }
    }

    private void setupInitialState() {
        try {
            if (btnPerpanjang != null) {
                btnPerpanjang.setDisable(true);
            }
            if (comboPerpanjang != null) {
                comboPerpanjang.setDisable(true);
            }
            if (comboDurasiMember != null) {
                comboDurasiMember.setDisable(true);
            }
            
            if (lblStatusCari != null) {
                lblStatusCari.setText("Silakan cari member terlebih dahulu");
                lblStatusCari.setStyle("-fx-text-fill: #e74c3c;");
            }
            
            if (lblLastUpdate != null) {
                lblLastUpdate.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
            }
            
            if (lblAdmin != null) {
                lblAdmin.setText("System Admin");
            }
            
            clearForm();
            
        } catch (Exception e) {
            System.err.println("Error setting up initial state: " + e.getMessage());
        }
    }

    private void searchMember() {
        String searchTerm = txtCariMember.getText().trim();
        
        if (searchTerm.isEmpty()) {
            lblStatusCari.setText("Masukkan ID Member atau Nama terlebih dahulu");
            lblStatusCari.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        try {
            List<Document> results = new ArrayList<>();
            
            System.out.println("Searching for: " + searchTerm);
            
            MongoCursor<Document> cursor = membersCollection.find(
                or(
                    regex("member_id", searchTerm, "i"),
                    regex("nama_lengkap", searchTerm, "i"),
                    eq("no_hp", searchTerm),
                    eq("email", searchTerm),
                    eq("nik", searchTerm)
                )
            ).iterator();
            
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
            cursor.close();
            
            System.out.println("Found " + results.size() + " results");
            
            if (!results.isEmpty()) {
                if (results.size() > 1) {
                    showAlert(Alert.AlertType.WARNING, "Multiple Results", 
                             "Ditemukan " + results.size() + " member. Menampilkan data pertama.");
                }
                
                Document memberDoc = results.get(0);
                
                System.out.println("=== FOUND MEMBER DATA ===");
                for (String key : memberDoc.keySet()) {
                    Object value = memberDoc.get(key);
                    System.out.println(key + ": " + value);
                }
                System.out.println("=========================");
                
                displayMemberData(memberDoc);
                lblStatusCari.setText("Member ditemukan: " + memberDoc.getString("nama_lengkap"));
                lblStatusCari.setStyle("-fx-text-fill: #27ae60;");
                
            } else {
                clearForm();
                lblStatusCari.setText("Member tidak ditemukan");
                lblStatusCari.setStyle("-fx-text-fill: #e74c3c;");
            }
            
        } catch (Exception e) {
            System.err.println("Error searching member: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Error saat mencari member: " + e.getMessage());
        }
    }

    private void displayMemberData(Document memberDoc) {
        try {
            currentMemberId = memberDoc.getObjectId("_id");
            
            // Basic member data
            String memberId = memberDoc.getString("member_id");
            txtIdMember.setText(memberId != null ? memberId : "N/A");
            
            txtNama.setText(memberDoc.getString("nama_lengkap"));
            txtTempatLahir.setText(memberDoc.getString("tempat_lahir"));
            
            // NIK
            String nik = memberDoc.getString("nik");
            if (nik != null && !nik.isEmpty()) {
                txtNIK.setText(nik);
            } else {
                txtNIK.setText("");
            }
            
            // Pekerjaan - LOAD DATA PEKERJAAN
            String pekerjaan = memberDoc.getString("pekerjaan");
            if (pekerjaan != null && !pekerjaan.isEmpty()) {
                comboPekerjaan.setValue(pekerjaan);
            } else {
                comboPekerjaan.setValue(null);
            }
            
            // Alamat
            String alamat = memberDoc.getString("alamat_domisili");
            txtAlamat.setText(alamat != null ? alamat : "");
            
            txtNoHp.setText(memberDoc.getString("no_hp"));
            txtEmail.setText(memberDoc.getString("email"));
            
            // Tanggal lahir
            String tglLahirStr = memberDoc.getString("tanggal_lahir");
            if (tglLahirStr != null) {
                try {
                    LocalDate tglLahir = LocalDate.parse(tglLahirStr);
                    dateTglLahir.setValue(tglLahir);
                    updateMemberPackages(tglLahir);
                } catch (Exception e) {
                    System.err.println("Error parsing tanggal_lahir: " + e.getMessage());
                }
            }
            
            // Jenis kelamin
            String jenisKelamin = memberDoc.getString("jenis_kelamin");
            if (jenisKelamin != null) {
                comboJenisKelamin.setValue(jenisKelamin);
            }
            
            // Durasi member
            String durasiMember = memberDoc.getString("durasi_member");
            if (durasiMember != null && comboDurasiMember != null) {
                comboDurasiMember.setValue(durasiMember);
                comboDurasiMember.setDisable(false);
            }
            
            // Photos
            loadPhotosFromDatabase(memberDoc);
            
            // Membership info
            displayMembershipInfo(memberDoc);
            
            // Update timestamp info
            updateSystemInfo(memberDoc);
            
            // Enable update button
            btnUpdate.setDisable(false);
            
        } catch (Exception e) {
            System.err.println("Error displaying member data: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Data Error", 
                     "Gagal menampilkan data member: " + e.getMessage());
        }
    }

    private void loadPhotosFromDatabase(Document memberDoc) {
        try {
            String fotoDiriBase64 = memberDoc.getString("foto_diri_base64");
            originalFotoBase64 = fotoDiriBase64;
            
            if (fotoDiriBase64 != null && !fotoDiriBase64.isEmpty()) {
                loadBase64Image(fotoDiriBase64, imgFotoDiri);
            } else {
                loadDefaultPhoto(imgFotoDiri);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading photos from database: " + e.getMessage());
            loadDefaultPhoto(imgFotoDiri);
        }
    }

    private void loadBase64Image(String base64String, ImageView imageView) {
        try {
            if (base64String != null && !base64String.isEmpty()) {
                String base64Data = base64String;
                if (base64String.contains(",")) {
                    base64Data = base64String.split(",")[1];
                }
                
                byte[] imageData = Base64.getDecoder().decode(base64Data);
                Image image = new Image(new ByteArrayInputStream(imageData));
                imageView.setImage(image);
            } else {
                loadDefaultPhoto(imageView);
            }
        } catch (Exception e) {
            System.err.println("Error loading base64 image: " + e.getMessage());
            loadDefaultPhoto(imageView);
        }
    }

    private void loadPhotoFromFile(String photoPath, ImageView imageView) {
        try {
            File photoFile = new File(photoPath);
            if (photoFile.exists()) {
                Image image = new Image(photoFile.toURI().toString());
                imageView.setImage(image);
                currentPhotoPath = photoPath;
            } else {
                loadDefaultPhoto(imageView);
            }
        } catch (Exception e) {
            System.err.println("Error loading photo from file: " + e.getMessage());
            loadDefaultPhoto(imageView);
        }
    }

    private void loadDefaultPhoto(ImageView imageView) {
        try {
            imageView.setImage(null);
            imageView.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc;");
            currentPhotoPath = null;
        } catch (Exception e) {
            System.err.println("Error loading default photo: " + e.getMessage());
        }
    }

    private void displayMembershipInfo(Document memberDoc) {
        try {
            String durasi = memberDoc.getString("durasi_member");
            String status = memberDoc.getString("status_keanggotaan");
            String masaBerlakuStr = memberDoc.getString("tanggal_berlaku_hingga_str");
            
            lblDurasiMember.setText(durasi != null ? durasi : "-");
            lblStatusMember.setText(status != null ? status : "-");
            
            if (masaBerlakuStr != null) {
                try {
                    masaAktifHingga = LocalDate.parse(masaBerlakuStr);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    lblMasaBerlaku.setText(masaAktifHingga.format(formatter));
                    
                    if (status != null && status.contains("Aktif")) {
                        btnPerpanjang.setDisable(false);
                        comboPerpanjang.setDisable(false);
                    } else {
                        btnPerpanjang.setDisable(true);
                        comboPerpanjang.setDisable(true);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing masa berlaku: " + e.getMessage());
                    lblMasaBerlaku.setText("-");
                    btnPerpanjang.setDisable(true);
                    comboPerpanjang.setDisable(true);
                }
            } else {
                lblMasaBerlaku.setText("-");
                btnPerpanjang.setDisable(true);
                comboPerpanjang.setDisable(true);
            }
        } catch (Exception e) {
            System.err.println("Error displaying membership info: " + e.getMessage());
            lblDurasiMember.setText("-");
            lblStatusMember.setText("Error");
            lblMasaBerlaku.setText("-");
            btnPerpanjang.setDisable(true);
            comboPerpanjang.setDisable(true);
        }
    }

    private void updateSystemInfo(Document memberDoc) {
        try {
            Date lastUpdate = memberDoc.getDate("last_updated");
            if (lastUpdate != null) {
                lblLastUpdate.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(lastUpdate));
            } else {
                Date createdAt = memberDoc.getDate("created_at");
                if (createdAt != null) {
                    lblLastUpdate.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(createdAt));
                } else {
                    lblLastUpdate.setText("-");
                }
            }
            
            lblAdmin.setText("System Admin");
            
        } catch (Exception e) {
            System.err.println("Error updating system info: " + e.getMessage());
        }
    }

    private void selectPhoto() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Pilih Foto Diri");
            
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp");
            fileChooser.getExtensionFilters().add(extFilter);
            
            File selectedFile = fileChooser.showOpenDialog(btnPilihFoto.getScene().getWindow());
            
            if (selectedFile != null) {
                currentPhotoPath = selectedFile.getAbsolutePath();
                loadPhotoFromFile(currentPhotoPath, imgFotoDiri);
            }
        } catch (Exception e) {
            System.err.println("Error selecting photo: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "File Error", 
                     "Gagal memilih foto: " + e.getMessage());
        }
    }

    private void updateMemberData() {
        if (currentMemberId == null) {
            showAlert(Alert.AlertType.WARNING, "Update Error", 
                     "Tidak ada member yang dipilih untuk diupdate");
            return;
        }

        try {
            if (!validateForm()) {
                return;
            }

            Document updateDoc = new Document();
            updateDoc.append("nama_lengkap", txtNama.getText().trim());
            updateDoc.append("tempat_lahir", txtTempatLahir.getText().trim());
            
            // NIK
            String nik = txtNIK.getText().trim();
            if (!nik.isEmpty()) {
                updateDoc.append("nik", nik);
            }
            
            // Pekerjaan - SIMPAN DATA PEKERJAAN
            if (comboPekerjaan.getValue() != null) {
                updateDoc.append("pekerjaan", comboPekerjaan.getValue());
                System.out.println("Saving pekerjaan: " + comboPekerjaan.getValue());
            }
            
            updateDoc.append("alamat_domisili", txtAlamat.getText().trim());
            updateDoc.append("no_hp", txtNoHp.getText().trim());
            updateDoc.append("email", txtEmail.getText().trim());
            updateDoc.append("jenis_kelamin", comboJenisKelamin.getValue());
            
            if (comboDurasiMember.getValue() != null) {
                String durasiMember = comboDurasiMember.getValue();
                updateDoc.append("durasi_member", durasiMember);
                updateDoc.append("durasi_bulan", getDurasiMemberBulan(durasiMember));
                updateDoc.append("dengan_pelatih", durasiMember.contains("+ Pelatih"));
                
                Date newExpiryDate = getTanggalBerlakuHingga(durasiMember);
                updateDoc.append("tanggal_berlaku_hingga", newExpiryDate);
                updateDoc.append("tanggal_berlaku_hingga_str", 
                    new SimpleDateFormat("yyyy-MM-dd").format(newExpiryDate));
            }
            
            if (dateTglLahir.getValue() != null) {
                String tglLahirStr = dateTglLahir.getValue().format(DateTimeFormatter.ISO_DATE);
                updateDoc.append("tanggal_lahir", tglLahirStr);
                
                LocalDate today = LocalDate.now();
                java.time.Period period = java.time.Period.between(dateTglLahir.getValue(), today);
                updateDoc.append("umur", period.getYears());
            }
            
            if (currentPhotoPath != null) {
                String base64FotoDiri = convertImageToBase64(currentPhotoPath);
                if (base64FotoDiri != null) {
                    updateDoc.append("foto_diri_base64", base64FotoDiri);
                    updateDoc.append("foto_diri_source", "file");
                    updateDoc.append("foto_diri_timestamp", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                    updateDoc.append("foto_diri_size", new File(currentPhotoPath).length());
                }
            }

            updateDoc.append("last_updated", new Date());

            System.out.println("=== UPDATE DOCUMENT ===");
            for (String key : updateDoc.keySet()) {
                Object value = updateDoc.get(key);
                System.out.println(key + ": " + value);
            }
            System.out.println("======================");

            UpdateResult result = membersCollection.updateOne(
                eq("_id", currentMemberId),
                new Document("$set", updateDoc)
            );

            if (result.getModifiedCount() > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                         "Data member berhasil diupdate");
                lblStatusCari.setText("Data berhasil diupdate - " + txtNama.getText());
                lblStatusCari.setStyle("-fx-text-fill: #27ae60;");
                
                lblLastUpdate.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                
                Document updatedMember = membersCollection.find(eq("_id", currentMemberId)).first();
                if (updatedMember != null) {
                    displayMembershipInfo(updatedMember);
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Update Info", 
                         "Tidak ada perubahan data yang dilakukan");
            }

        } catch (Exception e) {
            System.err.println("Error updating member data: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Update Error", 
                     "Gagal mengupdate data member: " + e.getMessage());
        }
    }

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

    private Date getTanggalBerlakuHingga(String durasiMember) {
        int durasiBulan = getDurasiMemberBulan(durasiMember);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, durasiBulan);
        return cal.getTime();
    }

    private String convertImageToBase64(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                FileInputStream imageInputStream = new FileInputStream(imageFile);
                byte[] imageBytes = new byte[(int) imageFile.length()];
                imageInputStream.read(imageBytes);
                imageInputStream.close();
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (IOException e) {
            System.err.println("Error converting image to base64: " + e.getMessage());
        }
        return null;
    }

    private void perpanjangMember() {
        if (currentMemberId == null) {
            showAlert(Alert.AlertType.WARNING, "Perpanjang Error", 
                     "Tidak ada member yang dipilih");
            return;
        }

        String selectedDuration = comboPerpanjang.getValue();
        if (selectedDuration == null) {
            showAlert(Alert.AlertType.WARNING, "Perpanjang Error", 
                     "Pilih durasi perpanjangan terlebih dahulu");
            return;
        }

        try {
            LocalDate newExpiryDate = calculateNewExpiryDate(selectedDuration);
            int durasiBulan = getDurasiMemberBulan(selectedDuration);
            
            Document membershipUpdate = new Document();
            membershipUpdate.append("durasi_member", selectedDuration);
            membershipUpdate.append("durasi_bulan", durasiBulan);
            membershipUpdate.append("dengan_pelatih", selectedDuration.contains("+ Pelatih"));
            membershipUpdate.append("status_keanggotaan", "VIP Aktif");
            membershipUpdate.append("tanggal_berlaku_hingga", 
                Date.from(newExpiryDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            membershipUpdate.append("tanggal_berlaku_hingga_str", 
                newExpiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            membershipUpdate.append("last_updated", new Date());

            UpdateResult result = membersCollection.updateOne(
                eq("_id", currentMemberId),
                new Document("$set", membershipUpdate)
            );

            if (result.getModifiedCount() > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                         "Membership berhasil diperpanjang hingga " + 
                         newExpiryDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                
                Document updatedMember = membersCollection.find(eq("_id", currentMemberId)).first();
                if (updatedMember != null) {
                    displayMembershipInfo(updatedMember);
                    comboDurasiMember.setValue(selectedDuration);
                    lblLastUpdate.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Perpanjang Error", 
                         "Gagal memperpanjang membership");
            }

        } catch (Exception e) {
            System.err.println("Error extending membership: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Perpanjang Error", 
                     "Gagal memperpanjang membership: " + e.getMessage());
        }
    }

    private LocalDate calculateNewExpiryDate(String duration) {
        LocalDate startDate = (masaAktifHingga != null && masaAktifHingga.isAfter(LocalDate.now())) 
            ? masaAktifHingga 
            : LocalDate.now();

        switch (duration) {
            case "1 Bulan":
                return startDate.plusMonths(1);
            case "3 Bulan":
                return startDate.plusMonths(3);
            case "6 Bulan":
                return startDate.plusMonths(6);
            case "1 Tahun":
                return startDate.plusYears(1);
            default:
                return startDate.plusMonths(1);
        }
    }

    private void resetForm() {
        clearForm();
        lblStatusCari.setText("Silakan cari member terlebih dahulu");
        lblStatusCari.setStyle("-fx-text-fill: #e74c3c;");
        txtCariMember.clear();
    }

    private void clearForm() {
        try {
            txtIdMember.clear();
            txtNama.clear();
            txtTempatLahir.clear();
            txtNIK.clear();
            txtAlamat.clear();
            txtNoHp.clear();
            txtEmail.clear();
            dateTglLahir.setValue(null);
            comboJenisKelamin.setValue(null);
            
            // Clear combo pekerjaan
            if (comboPekerjaan != null) {
                comboPekerjaan.setValue(null);
            }
            
            if (comboDurasiMember != null) {
                comboDurasiMember.setValue(null);
                comboDurasiMember.setDisable(true);
            }
            
            lblDurasiMember.setText("-");
            lblStatusMember.setText("-");
            lblMasaBerlaku.setText("-");
            
            imgFotoDiri.setImage(null);
            currentPhotoPath = null;
            currentMemberId = null;
            masaAktifHingga = null;
            currentAge = 0;
            originalFotoBase64 = null;
            
            btnUpdate.setDisable(true);
            btnPerpanjang.setDisable(true);
            comboPerpanjang.setDisable(true);
            comboPerpanjang.setValue(null);
            
        } catch (Exception e) {
            System.err.println("Error clearing form: " + e.getMessage());
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
        
        // Validasi NIK
        String nik = txtNIK.getText().trim();
        if (nik.isEmpty()) {
            errors.append("• NIK harus diisi\n");
        } else if (!nik.matches("\\d{16}")) {
            errors.append("• NIK harus 16 digit angka\n");
        }
        
        // Validasi Pekerjaan - VALIDASI BARU
        if (comboPekerjaan.getValue() == null) {
            errors.append("• Pekerjaan harus dipilih\n");
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

    private void kembaliKeMenuMember() {
        try {
            closeConnection();
            Stage currentStage = (Stage) imgkembali.getScene().getWindow();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_member/menu_member.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Menu Member");
            stage.setMaximized(true);
            
            currentStage.close();
            stage.show();
            
        } catch (Exception e) {
            System.err.println("Error navigating back to member menu: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Navigation Error", 
                     "Gagal memuat menu member: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        try {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            DialogPane dialogPane = alert.getDialogPane();
            try {
                URL stylesheet = getClass().getResource("/org/update_member/update_member.css");
                if (stylesheet != null) {
                    dialogPane.getStylesheets().add(stylesheet.toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("Error loading stylesheet for alert: " + e.getMessage());
            }
            
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error showing alert: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("MongoDB connection closed");
            }
        } catch (Exception e) {
            System.err.println("Error closing MongoDB connection: " + e.getMessage());
        }
    }
}