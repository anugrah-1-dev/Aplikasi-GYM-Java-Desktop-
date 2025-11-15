package org.openjfx;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.*;

public class MemberControllerT implements Initializable {

    // FXML Components
    @FXML private TextField fieldPencarian;
    @FXML private ComboBox<String> comboBulan;
    @FXML private Button btnResetFilter;
    @FXML private TableView<Member> tabelMember;
    @FXML private TableColumn<Member, Integer> colNo;
    @FXML private TableColumn<Member, ImageView> colFoto;
    @FXML private TableColumn<Member, String> colMemberId;
    @FXML private TableColumn<Member, String> colNIK;
    @FXML private TableColumn<Member, String> colNama;
    @FXML private TableColumn<Member, String> colJenisKelamin;
    @FXML private TableColumn<Member, String> colTanggalLahir;
    @FXML private TableColumn<Member, String> colEmail;
    @FXML private TableColumn<Member, String> colTelepon;
    @FXML private TableColumn<Member, String> colAlamat;
    @FXML private TableColumn<Member, String> colDurasiMember;
    @FXML private TableColumn<Member, String> colTanggalDaftar;
    @FXML private TableColumn<Member, String> colBerlakuHingga;
    @FXML private TableColumn<Member, String> colStatus;
    @FXML private TableColumn<Member, String> colAksi;
    @FXML private ImageView imgKembali;

    // Database components
    private MongoCollection<Document> memberCollection;
    private ObservableList<Member> memberList;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    
    // Logger
    private static final Logger logger = Logger.getLogger(MemberControllerT.class.getName());

    // Inner Member Class
    public static class Member {
        private ObjectId id;
        private String memberId;
        private String memberType;
        private String namaLengkap;
        private String tempatLahir;
        private LocalDate tanggalLahir;
        private String tanggalLahirDisplay;
        private int umur;
        private String jenisKelamin;
        private String nik;
        private String durasiMember;
        private int durasiBulan;
        private boolean denganPelathh;
        private LocalDate tanggalBerlakuHingga;
        private String tanggalBerlakuHinggaStr;
        private String alamatDomisili;
        private String noHp;
        private String email;
        private LocalDate tanggalDaftar;
        private String tanggalDaftarStr;
        private String waktuPendaftaran;
        private String fotoBase64;
        private String fotoDiriSource;
        private String fotoDiriTimestamp;
        private int fotoDiriSize;
        private String statusKeanggotaan;
        private LocalDate createdAt;

        // Getters and Setters
        public ObjectId getId() { return id; }
        public void setId(ObjectId id) { this.id = id; }

        public String getMemberId() { return memberId; }
        public void setMemberId(String memberId) { this.memberId = memberId; }

        public String getMemberType() { return memberType; }
        public void setMemberType(String memberType) { this.memberType = memberType; }

        public String getNamaLengkap() { return namaLengkap; }
        public void setNamaLengkap(String namaLengkap) { this.namaLengkap = namaLengkap; }

        public String getTempatLahir() { return tempatLahir; }
        public void setTempatLahir(String tempatLahir) { this.tempatLahir = tempatLahir; }

        public LocalDate getTanggalLahir() { return tanggalLahir; }
        public void setTanggalLahir(LocalDate tanggalLahir) { this.tanggalLahir = tanggalLahir; }

        public String getTanggalLahirDisplay() { return tanggalLahirDisplay; }
        public void setTanggalLahirDisplay(String tanggalLahirDisplay) { this.tanggalLahirDisplay = tanggalLahirDisplay; }

        public int getUmur() { return umur; }
        public void setUmur(int umur) { this.umur = umur; }

        public String getJenisKelamin() { return jenisKelamin; }
        public void setJenisKelamin(String jenisKelamin) { this.jenisKelamin = jenisKelamin; }

        public String getNik() { return nik; }
        public void setNik(String nik) { this.nik = nik; }

        public String getDurasiMember() { return durasiMember; }
        public void setDurasiMember(String durasiMember) { this.durasiMember = durasiMember; }

        public int getDurasiBulan() { return durasiBulan; }
        public void setDurasiBulan(int durasiBulan) { this.durasiBulan = durasiBulan; }

        public boolean isDenganPelathh() { return denganPelathh; }
        public void setDenganPelathh(boolean denganPelathh) { this.denganPelathh = denganPelathh; }

        public LocalDate getTanggalBerlakuHingga() { return tanggalBerlakuHingga; }
        public void setTanggalBerlakuHingga(LocalDate tanggalBerlakuHingga) { this.tanggalBerlakuHingga = tanggalBerlakuHingga; }

        public String getTanggalBerlakuHinggaStr() { return tanggalBerlakuHinggaStr; }
        public void setTanggalBerlakuHinggaStr(String tanggalBerlakuHinggaStr) { this.tanggalBerlakuHinggaStr = tanggalBerlakuHinggaStr; }

        public String getAlamatDomisili() { return alamatDomisili; }
        public void setAlamatDomisili(String alamatDomisili) { this.alamatDomisili = alamatDomisili; }

        public String getNoHp() { return noHp; }
        public void setNoHp(String noHp) { this.noHp = noHp; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDate getTanggalDaftar() { return tanggalDaftar; }
        public void setTanggalDaftar(LocalDate tanggalDaftar) { this.tanggalDaftar = tanggalDaftar; }

        public String getTanggalDaftarStr() { return tanggalDaftarStr; }
        public void setTanggalDaftarStr(String tanggalDaftarStr) { this.tanggalDaftarStr = tanggalDaftarStr; }

        public String getWaktuPendaftaran() { return waktuPendaftaran; }
        public void setWaktuPendaftaran(String waktuPendaftaran) { this.waktuPendaftaran = waktuPendaftaran; }

        public String getFotoBase64() { return fotoBase64; }
        public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }

        public String getFotoDiriSource() { return fotoDiriSource; }
        public void setFotoDiriSource(String fotoDiriSource) { this.fotoDiriSource = fotoDiriSource; }

        public String getFotoDiriTimestamp() { return fotoDiriTimestamp; }
        public void setFotoDiriTimestamp(String fotoDiriTimestamp) { this.fotoDiriTimestamp = fotoDiriTimestamp; }

        public int getFotoDiriSize() { return fotoDiriSize; }
        public void setFotoDiriSize(int fotoDiriSize) { this.fotoDiriSize = fotoDiriSize; }

        public String getStatusKeanggotaan() { return statusKeanggotaan; }
        public void setStatusKeanggotaan(String statusKeanggotaan) { this.statusKeanggotaan = statusKeanggotaan; }

        public LocalDate getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            initializeDatabase();
            initializeComboBox();
            initializeTable();
            loadMemberData();
            setupEventHandlers();
            System.out.println("✓ MemberControllerT initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing MemberControllerT", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error", 
                    "Gagal menginisialisasi controller: " + e.getMessage());
        }
    }

    private void initializeDatabase() {
        try {
            MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
            MongoDatabase database = mongoClient.getDatabase("gym");
            this.memberCollection = database.getCollection("data_members");
            System.out.println("✓ Connected to MongoDB successfully");
        } catch (Exception e) {
            System.err.println("✗ Error connecting to MongoDB: " + e.getMessage());
            showAlert("Database Error", "Gagal terhubung ke database: " + e.getMessage());
        }
    }

    private void initializeComboBox() {
        String[] bulan = {"Januari", "Februari", "Maret", "April", "Mei", "Juni", 
                         "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
        comboBulan.setItems(FXCollections.observableArrayList(bulan));
    }

    private void initializeTable() {
        // Set up table columns
        colNo.setCellValueFactory(cellData -> {
            int index = tabelMember.getItems().indexOf(cellData.getValue()) + 1;
            return javafx.beans.binding.Bindings.createObjectBinding(() -> index);
        });

        colMemberId.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        colNIK.setCellValueFactory(new PropertyValueFactory<>("nik"));
        colNama.setCellValueFactory(new PropertyValueFactory<>("namaLengkap"));
        colJenisKelamin.setCellValueFactory(new PropertyValueFactory<>("jenisKelamin"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelepon.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        colAlamat.setCellValueFactory(new PropertyValueFactory<>("alamatDomisili"));
        colDurasiMember.setCellValueFactory(new PropertyValueFactory<>("durasiMember"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusKeanggotaan"));

        // Format tanggal untuk display
        colTanggalLahir.setCellValueFactory(cellData -> {
            LocalDate tanggal = cellData.getValue().getTanggalLahir();
            return new javafx.beans.property.SimpleStringProperty(
                tanggal != null ? tanggal.format(displayFormatter) : "-"
            );
        });

        colTanggalDaftar.setCellValueFactory(cellData -> {
            LocalDate tanggal = cellData.getValue().getTanggalDaftar();
            return new javafx.beans.property.SimpleStringProperty(
                tanggal != null ? tanggal.format(displayFormatter) : "-"
            );
        });

        colBerlakuHingga.setCellValueFactory(cellData -> {
            LocalDate tanggal = cellData.getValue().getTanggalBerlakuHingga();
            return new javafx.beans.property.SimpleStringProperty(
                tanggal != null ? tanggal.format(displayFormatter) : "-"
            );
        });

        // Kolom foto dari base64
        colFoto.setCellFactory(param -> new TableCell<Member, ImageView>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-border-radius: 20; -fx-background-radius: 20;");
            }

            @Override
            protected void updateItem(ImageView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                } else {
                    Member member = getTableView().getItems().get(getIndex());
                    if (member != null && member.getFotoBase64() != null && 
                        !member.getFotoBase64().isEmpty()) {
                        try {
                            // Handle base64 string yang mungkin memiliki prefix
                            String base64Data = member.getFotoBase64();
                            if (base64Data.contains(",")) {
                                base64Data = base64Data.split(",")[1];
                            }
                            
                            byte[] imageData = Base64.getDecoder().decode(base64Data);
                            Image image = new Image(new ByteArrayInputStream(imageData));
                            imageView.setImage(image);
                            setGraphic(imageView);
                        } catch (Exception e) {
                            System.err.println("Error loading member photo: " + e.getMessage());
                            setDefaultAvatar();
                        }
                    } else {
                        setDefaultAvatar();
                    }
                }
            }

            private void setDefaultAvatar() {
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-avatar.png"));
                    if (defaultImage.isError()) {
                        setGraphic(null);
                    } else {
                        imageView.setImage(defaultImage);
                        setGraphic(imageView);
                    }
                } catch (Exception e) {
                    setGraphic(null);
                }
            }
        });

        // Kolom aksi (Hanya Hapus)
        colAksi.setCellFactory(param -> new TableCell<Member, String>() {
            private final Button btnHapus = new Button("Hapus");
            private final HBox hbox = new HBox(btnHapus);

            {
                btnHapus.setOnAction(event -> {
                    Member member = getTableView().getItems().get(getIndex());
                    deleteMember(member);
                });

                btnHapus.getStyleClass().add("button-delete");
                hbox.setStyle("-fx-alignment: center;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
    }

    // Database Methods
    private List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        
        if (memberCollection == null) {
            showAlert("Error", "Koneksi database tidak terbentuk");
            return members;
        }
        
        try (MongoCursor<Document> cursor = memberCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Member member = convertDocumentToMember(doc);
                if (member != null) {
                    members.add(member);
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Error getting all members: " + e.getMessage());
            showAlert("Error", "Gagal memuat data member: " + e.getMessage());
        }
        
        System.out.println("✓ Loaded " + members.size() + " members from database");
        return members;
    }

    private List<Member> searchMembers(String keyword) {
        List<Member> members = new ArrayList<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllMembers();
        }

        String searchPattern = ".*" + keyword + ".*";
        
        try {
            FindIterable<Document> results = memberCollection.find(
                or(
                    regex("member_id", searchPattern, "i"),
                    regex("nama_lengkap", searchPattern, "i"),
                    regex("email", searchPattern, "i"),
                    regex("no_hp", searchPattern, "i"),
                    regex("nik", searchPattern, "i")
                )
            );

            try (MongoCursor<Document> cursor = results.iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    Member member = convertDocumentToMember(doc);
                    if (member != null) {
                        members.add(member);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Error searching members: " + e.getMessage());
        }
        
        System.out.println("✓ Found " + members.size() + " members for search: " + keyword);
        return members;
    }

    private List<Member> filterMembersByMonth(int month, int year) {
        List<Member> members = new ArrayList<>();
        
        try {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            
            String startDateStr = startDate.format(dateFormatter);
            String endDateStr = endDate.format(dateFormatter);
            
            FindIterable<Document> results = memberCollection.find(
                and(
                    gte("tanggal_daftar_str", startDateStr),
                    lte("tanggal_daftar_str", endDateStr)
                )
            );

            try (MongoCursor<Document> cursor = results.iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    Member member = convertDocumentToMember(doc);
                    if (member != null) {
                        members.add(member);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Error filtering members by month: " + e.getMessage());
        }
        
        System.out.println("✓ Filtered " + members.size() + " members for month: " + month);
        return members;
    }

    private boolean deleteMemberFromDatabase(String id) {
        try {
            memberCollection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            System.out.println("✓ Deleted member with ID: " + id);
            return true;
        } catch (Exception e) {
            System.err.println("✗ Error deleting member: " + e.getMessage());
            return false;
        }
    }

    // Helper method untuk convert Date ke LocalDate
    private LocalDate convertToLocalDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        
        try {
            if (dateObj instanceof Date) {
                return ((Date) dateObj).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            } else if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                // Coba berbagai format
                try {
                    return LocalDate.parse(dateStr, dateFormatter);
                } catch (DateTimeParseException e1) {
                    try {
                        DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        return LocalDate.parse(dateStr, altFormatter);
                    } catch (DateTimeParseException e2) {
                        try {
                            DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
                            return LocalDate.parse(dateStr, isoFormatter);
                        } catch (DateTimeParseException e3) {
                            System.err.println("⚠ Cannot parse date string: " + dateStr);
                            return null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠ Error converting to LocalDate: " + e.getMessage());
        }
        
        return null;
    }

    // Helper method untuk convert Date ke LocalDateTime
    private LocalDateTime convertToLocalDateTime(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        
        try {
            if (dateObj instanceof Date) {
                return ((Date) dateObj).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            } else if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                try {
                    return LocalDateTime.parse(dateStr, dateTimeFormatter);
                } catch (DateTimeParseException e) {
                    System.err.println("⚠ Cannot parse datetime string: " + dateStr);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("⚠ Error converting to LocalDateTime: " + e.getMessage());
        }
        
        return null;
    }

    // Conversion Methods
    private Member convertDocumentToMember(Document doc) {
        Member member = new Member();
        
        try {
            member.setId(doc.getObjectId("_id"));
            
            // Handle semua field dengan safe get
            member.setMemberId(getSafeString(doc, "member_id"));
            member.setMemberType(getSafeString(doc, "member_type"));
            member.setNamaLengkap(getSafeString(doc, "nama_lengkap"));
            member.setTempatLahir(getSafeString(doc, "tempat_lahir"));
            
            // Tanggal lahir - handle both Date and String
            Object tanggalLahirObj = doc.get("tanggal_lahir");
            member.setTanggalLahir(convertToLocalDate(tanggalLahirObj));
            
            member.setTanggalLahirDisplay(getSafeString(doc, "tanggal_lahir_display"));
            
            // Umur
            Object umurObj = doc.get("umur");
            if (umurObj instanceof Integer) {
                member.setUmur((Integer) umurObj);
            } else if (umurObj instanceof String) {
                try {
                    member.setUmur(Integer.parseInt((String) umurObj));
                } catch (NumberFormatException e) {
                    member.setUmur(0);
                }
            } else {
                member.setUmur(0);
            }
            
            member.setJenisKelamin(getSafeString(doc, "jenis_kelamin"));
            member.setNik(getSafeString(doc, "nik"));
            member.setDurasiMember(getSafeString(doc, "durasi_member"));
            
            // Durasi bulan
            Object durasiBulanObj = doc.get("durasi_bulan");
            if (durasiBulanObj instanceof Integer) {
                member.setDurasiBulan((Integer) durasiBulanObj);
            } else if (durasiBulanObj instanceof String) {
                try {
                    member.setDurasiBulan(Integer.parseInt((String) durasiBulanObj));
                } catch (NumberFormatException e) {
                    member.setDurasiBulan(1);
                }
            } else {
                member.setDurasiBulan(1);
            }
            
            // Dengan pelathh
            Object denganPelathhObj = doc.get("dengan_pelathh");
            if (denganPelathhObj instanceof Boolean) {
                member.setDenganPelathh((Boolean) denganPelathhObj);
            } else if (denganPelathhObj instanceof String) {
                member.setDenganPelathh(Boolean.parseBoolean((String) denganPelathhObj));
            } else {
                member.setDenganPelathh(false);
            }
            
            // Tanggal berlaku hingga
            Object berlakuHinggaObj = doc.get("tanggal_berlaku_hingga");
            LocalDateTime berlakuHinggaDateTime = convertToLocalDateTime(berlakuHinggaObj);
            if (berlakuHinggaDateTime != null) {
                member.setTanggalBerlakuHingga(berlakuHinggaDateTime.toLocalDate());
            }
            
            member.setTanggalBerlakuHinggaStr(getSafeString(doc, "tanggal_berlaku_hingga_str"));
            member.setAlamatDomisili(getSafeString(doc, "alamat_domisili"));
            member.setNoHp(getSafeString(doc, "no_hp"));
            member.setEmail(getSafeString(doc, "email"));
            
            // Tanggal daftar
            Object tanggalDaftarObj = doc.get("tanggal_daftar");
            LocalDateTime tanggalDaftarDateTime = convertToLocalDateTime(tanggalDaftarObj);
            if (tanggalDaftarDateTime != null) {
                member.setTanggalDaftar(tanggalDaftarDateTime.toLocalDate());
            }
            
            member.setTanggalDaftarStr(getSafeString(doc, "tanggal_daftar_str"));
            member.setWaktuPendaftaran(getSafeString(doc, "waktu_pendaftaran"));
            
            // Handle foto - coba beberapa field yang mungkin
            String fotoBase64 = getSafeString(doc, "foto_diri_base64");
            if (fotoBase64 == null || fotoBase64.isEmpty()) {
                fotoBase64 = getSafeString(doc, "foto_base64");
            }
            member.setFotoBase64(fotoBase64);
            
            member.setFotoDiriSource(getSafeString(doc, "foto_diri_source"));
            member.setFotoDiriTimestamp(getSafeString(doc, "foto_diri_timestamp"));
            
            // Foto size
            Object fotoSizeObj = doc.get("foto_diri_size");
            if (fotoSizeObj instanceof Integer) {
                member.setFotoDiriSize((Integer) fotoSizeObj);
            } else if (fotoSizeObj instanceof String) {
                try {
                    member.setFotoDiriSize(Integer.parseInt((String) fotoSizeObj));
                } catch (NumberFormatException e) {
                    member.setFotoDiriSize(0);
                }
            } else {
                member.setFotoDiriSize(0);
            }
            
            member.setStatusKeanggotaan(getSafeString(doc, "status_keanggotaan"));
            
            // Created at
            Object createdAtObj = doc.get("created_at");
            LocalDateTime createdAtDateTime = convertToLocalDateTime(createdAtObj);
            if (createdAtDateTime != null) {
                member.setCreatedAt(createdAtDateTime.toLocalDate());
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error converting document to member: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        
        return member;
    }

    // Safe method untuk mendapatkan string dari document
    private String getSafeString(Document doc, String key) {
        try {
            Object value = doc.get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof Date) {
                // Convert Date to string representation
                Date date = (Date) value;
                return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(dateFormatter);
            } else {
                return value.toString();
            }
        } catch (Exception e) {
            System.err.println("⚠ Error getting string for key " + key + ": " + e.getMessage());
            return null;
        }
    }

    // Controller Methods
    private void loadMemberData() {
        List<Member> members = getAllMembers();
        memberList = FXCollections.observableArrayList(members);
        tabelMember.setItems(memberList);
        System.out.println("✓ Displaying " + memberList.size() + " members in table");
    }

    private void setupEventHandlers() {
        // Pencarian real-time
        fieldPencarian.textProperty().addListener((observable, oldValue, newValue) -> {
            searchMembersHandler(newValue);
        });

        // Filter bulan
        comboBulan.setOnAction(event -> {
            filterByMonthHandler();
        });
    }

    private void searchMembersHandler(String keyword) {
        List<Member> results = searchMembers(keyword);
        memberList = FXCollections.observableArrayList(results);
        tabelMember.setItems(memberList);
    }

    private void filterByMonthHandler() {
        String selectedMonth = comboBulan.getValue();
        if (selectedMonth != null && !selectedMonth.isEmpty()) {
            int month = comboBulan.getItems().indexOf(selectedMonth) + 1;
            int year = LocalDate.now().getYear();
            
            List<Member> results = filterMembersByMonth(month, year);
            memberList = FXCollections.observableArrayList(results);
            tabelMember.setItems(memberList);
        }
    }

    @FXML
    private void resetFilter() {
        fieldPencarian.clear();
        comboBulan.getSelectionModel().clearSelection();
        loadMemberData();
        System.out.println("✓ Filters reset");
    }

    @FXML
    private void kembaliKeDashboard(MouseEvent event) {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Konfirmasi");
            confirmation.setHeaderText("Kembali ke Menu Utama?");
            confirmation.setContentText("Apakah Anda yakin ingin kembali ke menu utama?");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Coba beberapa path yang mungkin
                String[] possiblePaths = {
                    "/org/dashboard_penjaga/dashboard.fxml",
                    "/dashboard.fxml", 
                    "/dahsboard.fxml",
                    "/org/dahsboard_penjaga/dahsboard.fxml"
                };
                
                for (String path : possiblePaths) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                        Parent root = loader.load();
                        Stage stage = (Stage) imgKembali.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                        System.out.println("✓ Successfully loaded dashboard from: " + path);
                        return;
                    } catch (IOException e) {
                        System.out.println("✗ Failed to load from: " + path);
                    }
                }
                
                showAlert(Alert.AlertType.ERROR, "Error", "Tidak dapat menemukan file dashboard");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Gagal kembali ke menu utama", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Gagal memuat halaman menu utama: " + e.getMessage());
        }
    }

    private void deleteMember(Member member) {
        if (member == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Member");
        alert.setContentText("Apakah Anda yakin ingin menghapus member: " + member.getNamaLengkap() + "?\nTindakan ini tidak dapat dibatalkan!");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = deleteMemberFromDatabase(member.getId().toString());
                    if (success) {
                        memberList.remove(member);
                        showAlert("Sukses", "Member " + member.getNamaLengkap() + " berhasil dihapus");
                    } else {
                        showAlert("Error", "Gagal menghapus member " + member.getNamaLengkap());
                    }
                } catch (Exception e) {
                    showAlert("Error", "Gagal menghapus member: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Method untuk refresh data dari luar
    public void refreshData() {
        loadMemberData();
    }
}