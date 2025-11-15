package org.openjfx;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ControllerBuatAkun implements Initializable {

    // Komponen Tabel
    @FXML private TableView<User> tableView;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> passwordColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> idLoginColumn;
    
    // Komponen Form
    @FXML private TextField idLoginField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    
    // Komponen Pencarian
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;
    
    // Tombol Aksi
    @FXML private Button createButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button searchButton;
    
    // Komponen tambahan
    @FXML private CheckBox showPasswordCheckBox;

    @FXML private ImageView backIcon; // Sesuaikan dengan fx:id di FXML
    
    // Koneksi Database
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> usersCollection;
    
    // Data untuk tabel
    private ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDatabaseConnection();
        setupTableColumns();
        setupComboBoxes();
        loadUserData();
        
        // Set selection mode untuk tabel
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    
    private void setupDatabaseConnection() {
        try {
            String connectionString = "mongodb://localhost:27017";
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase("sistem_parkir");
            usersCollection = database.getCollection("login");
            System.out.println("Terhubung ke MongoDB");
        } catch (Exception e) {
            showAlert("Error Database", "Gagal terhubung ke MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        idLoginColumn.setCellValueFactory(new PropertyValueFactory<>("idLogin"));
        
        tableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    fillFormWithSelectedUser(newSelection);
                }
            });
    }
    
    private void setupComboBoxes() {
        roleComboBox.getItems().addAll("admin", "penjaga", "manager");
        filterComboBox.getItems().addAll("Semua", "Username", "Role", "ID Login");
        filterComboBox.getSelectionModel().selectFirst();
    }
    
    private void loadUserData() {
        userList.clear();
        FindIterable<Document> documents = usersCollection.find();
        
        for (Document doc : documents) {
            User user = new User(
                doc.getString("username"),
                doc.getString("password"),
                doc.getString("role"),
                doc.getString("id login")
            );
            userList.add(user);
        }
        
        tableView.setItems(userList);
    }
    
    @FXML
    private void handleCreateAccount() {
        if (validateForm()) {
            Document newUser = new Document()
                .append("username", usernameField.getText())
                .append("password", passwordField.getText())
                .append("role", roleComboBox.getValue().toLowerCase())
                .append("id login", idLoginField.getText());
            
            try {
                usersCollection.insertOne(newUser);
                showAlert("Sukses", "Akun berhasil dibuat!");
                clearForm();
                loadUserData();
            } catch (Exception e) {
                showAlert("Error", "Gagal membuat akun: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleUpdateAccount() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            showAlert("Error", "Pilih user yang akan diupdate dari tabel");
            return;
        }
        
        if (validateForm()) {
            try {
                Bson filter = Filters.and(
                    Filters.eq("username", selectedUser.getUsername()),
                    Filters.eq("id login", selectedUser.getIdLogin())
                );
                
                Bson updates = Updates.combine(
                    Updates.set("username", usernameField.getText()),
                    Updates.set("password", passwordField.getText()),
                    Updates.set("role", roleComboBox.getValue().toLowerCase()),
                    Updates.set("id login", idLoginField.getText())
                );
                
                UpdateResult result = usersCollection.updateOne(filter, updates);
                if (result.getModifiedCount() > 0) {
                    showAlert("Sukses", "Akun berhasil diperbarui!");
                    clearForm();
                    loadUserData();
                } else {
                    showAlert("Info", "Tidak ada perubahan data");
                }
            } catch (Exception e) {
                showAlert("Error", "Gagal memperbarui akun: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        String filterType = filterComboBox.getValue();
        
        if (filterType.equals("Semua")) {
            loadUserData();
            return;
        }
        
        Bson filter;
        
        switch (filterType) {
            case "Username":
                filter = Filters.regex("username", searchTerm, "i");
                break;
            case "Role":
                filter = Filters.eq("role", searchTerm.toLowerCase());
                break;
            case "ID Login":
                filter = Filters.eq("id login", searchTerm);
                break;
            default:
                loadUserData();
                return;
        }
        
        userList.clear();
        FindIterable<Document> results = usersCollection.find(filter);
        
        for (Document doc : results) {
            User user = new User(
                doc.getString("username"),
                doc.getString("password"),
                doc.getString("role"),
                doc.getString("id login")
            );
            userList.add(user);
        }
        
        if (userList.isEmpty()) {
            showAlert("Info", "Tidak ditemukan data yang sesuai");
        }
    }
    
    @FXML
    private void handleDelete() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            showAlert("Error", "Pilih user yang akan dihapus");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Konfirmasi Penghapusan");
        confirmation.setHeaderText("Hapus Akun User");
        confirmation.setContentText("Anda yakin ingin menghapus akun ini?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Bson filter = Filters.and(
                        Filters.eq("username", selectedUser.getUsername()),
                        Filters.eq("id login", selectedUser.getIdLogin())
                    );
                    usersCollection.deleteOne(filter);
                    showAlert("Sukses", "Akun berhasil dihapus!");
                    loadUserData();
                } catch (Exception e) {
                    showAlert("Error", "Gagal menghapus akun: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    @FXML
    private void handleShowPassword() {
        if (showPasswordCheckBox.isSelected()) {
            passwordField.setPromptText(passwordField.getText());
            passwordField.clear();
        } else {
            passwordField.setText(passwordField.getPromptText());
            passwordField.setPromptText("Masukkan Password");
        }
    }
    
    private void fillFormWithSelectedUser(User user) {
        idLoginField.setText(user.getIdLogin());
        usernameField.setText(user.getUsername());
        passwordField.setText(user.getPassword());
        roleComboBox.setValue(user.getRole());
    }


    @FXML
private void handleBackButton(MouseEvent event) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/dashboard_admin/dashboardAdmin.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) backIcon.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    } catch (IOException e) {
        showAlert("Error", "Gagal kembali ke dashboard: " + e.getMessage());
        e.printStackTrace();
    }
}

    
    private boolean validateForm() {
        if (usernameField.getText().trim().isEmpty()) {
            showAlert("Error", "Username tidak boleh kosong");
            return false;
        }
        
        if (passwordField.getText().trim().isEmpty()) {
            showAlert("Error", "Password tidak boleh kosong");
            return false;
        }
        
        if (roleComboBox.getValue() == null) {
            showAlert("Error", "Pilih role untuk user");
            return false;
        }
        
        if (idLoginField.getText().trim().isEmpty()) {
            showAlert("Error", "ID Login tidak boleh kosong");
            return false;
        }
        
        return true;
    }
    
    private void clearForm() {
        idLoginField.clear();
        usernameField.clear();
        passwordField.clear();
        roleComboBox.getSelectionModel().clearSelection();
        tableView.getSelectionModel().clearSelection();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static class User {
        private final String username;
        private final String password;
        private final String role;
        private final String idLogin;
        
        public User(String username, String password, String role, String idLogin) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.idLogin = idLogin;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public String getIdLogin() { return idLogin; }
    }
}