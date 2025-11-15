package org.openjfx;

import java.io.IOException;
import java.util.function.UnaryOperator;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.IntegerStringConverter;

public class RevisiHargaParkirController {

    @FXML private ComboBox<String> jenisKendaraanComboBox;
    @FXML private TextField hargaParkirField;
    @FXML private Button perbaruiButton;
    @FXML private Button backButton;
    @FXML private ImageView imgkembali;
    
    private MongoCollection<Document> hargaKarcisCollection;

    @FXML
    private void initialize() {
        // Initialize database connection
        this.hargaKarcisCollection = DatabaseConnector.getHargaKarcisCollection();
        
        // Initialize ComboBox with vehicle types
        jenisKendaraanComboBox.getItems().addAll("motor", "mobil");
        
        // Set up numeric input filter
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            return newText.matches("\\d*") ? change : null;
        };
        hargaParkirField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, filter));
        
        // Set up event handlers
        jenisKendaraanComboBox.setOnAction(event -> loadHargaForSelectedVehicle());
        perbaruiButton.setOnAction(event -> handlePerbarui());
        backButton.setOnAction(event -> loadPage("/org/dashboard_admin/dashboardAdmin.fxml"));
        imgkembali.setOnMouseClicked(event -> loadPage("/org/dashboard_admin/dashboardAdmin.fxml"));
    }

    private void loadHargaForSelectedVehicle() {
        String selectedVehicle = jenisKendaraanComboBox.getValue();
        if (selectedVehicle != null && hargaKarcisCollection != null) {
            Document doc = hargaKarcisCollection.find(Filters.eq("jenis_kendaraan", selectedVehicle)).first();
            hargaParkirField.setText(doc != null ? String.valueOf(doc.getInteger("harga")) : "");
        }
    }
    
    private void loadPage(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Gagal memuat halaman: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePerbarui() {
        String jenis = jenisKendaraanComboBox.getValue();
        String hargaText = hargaParkirField.getText().trim();
        
        if (jenis == null || jenis.isEmpty()) {
            showAlert("Error", "Pilih jenis kendaraan terlebih dahulu");
            return;
        }
        
        if (hargaText.isEmpty()) {
            showAlert("Error", "Masukkan harga parkir");
            return;
        }
        
        try {
            int harga = Integer.parseInt(hargaText);
            if (harga <= 0) {
                showAlert("Error", "Harga harus lebih besar dari 0");
                return;
            }
            
            Document existing = hargaKarcisCollection.find(Filters.eq("jenis_kendaraan", jenis)).first();
            if (existing != null) {
                hargaKarcisCollection.updateOne(Filters.eq("jenis_kendaraan", jenis), Updates.set("harga", harga));
            } else {
                hargaKarcisCollection.insertOne(new Document("jenis_kendaraan", jenis).append("harga", harga));
            }
            
            showAlert("Sukses", String.format("Harga %s berhasil diperbarui menjadi Rp %,d", jenis, harga));
        } catch (NumberFormatException e) {
            showAlert("Error", "Masukkan harga yang valid (angka)");
        } catch (Exception e) {
            showAlert("Error", "Gagal memperbarui harga: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message) {
        Window owner = perbaruiButton.getScene().getWindow();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
 
    public void start(Stage primaryStage) throws Exception {
        // Initialize database connection
        DatabaseConnector.initialize();
        
        Parent root = FXMLLoader.load(getClass().getResource("/org/revisi/revisi.fxml"));
        primaryStage.setTitle("Revisi Harga Parkir");
        primaryStage.setScene(new Scene(root, 1311, 630));
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public void stop() {
        DatabaseConnector.close();
    }
}