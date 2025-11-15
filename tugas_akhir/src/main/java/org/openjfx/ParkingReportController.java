package org.openjfx;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.openjfx.ParkingReportController.ParkingTransaction;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class ParkingReportController {

    // UI Components
    @FXML private TextField idTransaksiField;
    @FXML private Button searchButton;
    @FXML private Button resetButton;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TableView<ParkingTransaction> tableView;
    @FXML private TableColumn<ParkingTransaction, Integer> noColumn;
    @FXML private TableColumn<ParkingTransaction, String> idKarcisColumn;
    @FXML private TableColumn<ParkingTransaction, String> jenisKendaraanColumn;
    @FXML private TableColumn<ParkingTransaction, String> waktuMasukColumn;
    @FXML private TableColumn<ParkingTransaction, String> waktuKeluarColumn;
    @FXML private TableColumn<ParkingTransaction, String> durasiColumn;
    @FXML private TableColumn<ParkingTransaction, Double> totalBayarColumn;
    @FXML private TableColumn<ParkingTransaction, String> tanggalTransaksiColumn;
    @FXML private Button printButton;
    @FXML private ImageView logoImageView, imgKembali;
    @FXML private Button backButton;
    @FXML private DatePicker endDatePicker;
    @FXML private DatePicker startDatePicker;
    @FXML private Button dateFilterButton;


    // Data
    private final ParkingDataService dataService = new ParkingDataService();
    private final ObservableList<ParkingTransaction> transactions = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupTableColumns();
        setupFilterComboBox();
        setupButtonActions();
        loadInitialData();
        setupDatePickers();
    }

        private void setupDatePickers() {
        // Set default values (optional)
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        
        dateFilterButton.setOnAction(event -> handleDateFilter());
    }

    private void handleDateFilter() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showAlert("Peringatan", "Silakan pilih tanggal awal dan akhir");
            return;
        }
        
        if (startDate.isAfter(endDate)) {
            showAlert("Peringatan", "Tanggal awal tidak boleh setelah tanggal akhir");
            return;
        }
        
        refreshTableWithDateRange(startDate, endDate);
    }

    private void refreshTableWithDateRange(LocalDate startDate, LocalDate endDate) {
        List<Document> documents = dataService.fetchTransactions(
            null, 
            null, 
            startDate, 
            endDate
        );
        
        transactions.setAll(documents.stream()
            .map(this::convertDocumentToTransaction)
            .collect(Collectors.toList()));
        
        for (int i = 0; i < transactions.size(); i++) {
            transactions.get(i).setNo(i + 1);
        }
    }

    // Modifikasi method refreshTable yang sudah ada
    private void refreshTable(String idTransaksi, String filterType) {
        List<Document> documents = dataService.fetchTransactions(
            idTransaksi, 
            filterType,
            null,
            null
        );
        
        transactions.setAll(documents.stream()
            .map(this::convertDocumentToTransaction)
            .collect(Collectors.toList()));
        
        for (int i = 0; i < transactions.size(); i++) {
            transactions.get(i).setNo(i + 1);
        }
    }
        
    private void setupTableColumns() {
        noColumn.setCellValueFactory(new PropertyValueFactory<>("no"));
        idKarcisColumn.setCellValueFactory(new PropertyValueFactory<>("idKarcis"));
        jenisKendaraanColumn.setCellValueFactory(new PropertyValueFactory<>("jenisKendaraan"));
        waktuMasukColumn.setCellValueFactory(new PropertyValueFactory<>("waktuMasuk"));
        waktuKeluarColumn.setCellValueFactory(new PropertyValueFactory<>("waktuKeluar"));
        durasiColumn.setCellValueFactory(new PropertyValueFactory<>("durasi"));
        totalBayarColumn.setCellValueFactory(new PropertyValueFactory<>("totalBayar"));
        
        totalBayarColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Rp %,.0f", item));
                }
            }
        });
        
        tanggalTransaksiColumn.setCellValueFactory(new PropertyValueFactory<>("tanggalTransaksi"));
        tableView.setItems(transactions);
    }

    private void setupFilterComboBox() {
        filterComboBox.setItems(FXCollections.observableArrayList(
            "Semua", "Harian", "Mingguan", "Bulanan"
        ));
        filterComboBox.getSelectionModel().selectFirst();
    }

    private void setupButtonActions() {
        // Search and Filter
        searchButton.setOnAction(event -> handleSearch());
        resetButton.setOnAction(event -> handleReset());
        filterComboBox.setOnAction(event -> handleFilterChange());
        
        // Navigation
        backButton.setOnAction(event -> loadPage("/org/dashboard_admin/dashboardAdmin.fxml"));
        imgKembali.setOnMouseClicked(e -> loadPage("/org/dashboard_admin/dashboardAdmin.fxml"));
        
        // Printing
        printButton.setOnAction(event -> handlePrint());

    }
    
    private void handlePrint() {
        System.out.println("Print button clicked");
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job == null) {
                showAlert("Print Error", "Tidak dapat membuat printer job");
                return;
            }
            
            // List available printers for debugging
            System.out.println("Available printers:");
            Printer.getAllPrinters().forEach(p -> System.out.println("- " + p.getName()));
            
            Printer printer = Printer.getDefaultPrinter();
            if (printer == null) {
                showAlert("Print Error", "Tidak ada printer yang terdeteksi");
                return;
            }
            
            System.out.println("Using printer: " + printer.getName());
            
            // Configure page layout
            PageLayout pageLayout = printer.createPageLayout(
                Paper.A4, 
                PageOrientation.LANDSCAPE,
                Printer.MarginType.DEFAULT);
            
            // Show print dialog
            boolean proceed = job.showPrintDialog(tableView.getScene().getWindow());
            if (!proceed) {
                System.out.println("Print cancelled by user");
                return;
            }
            
            // Print the table
            boolean success = job.printPage(pageLayout, tableView);
            if (success) {
                job.endJob();
                showAlert("Print Success", "Dokumen berhasil dikirim ke printer");
            } else {
                showAlert("Print Error", "Gagal mencetak dokumen");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Print Error", "Terjadi kesalahan saat mencetak: " + e.getMessage());
        }
    }

    private void loadInitialData() {
        refreshTable(null, null);
    }

    private void handleSearch() {
        String id = idTransaksiField.getText().trim();
        if (!id.isEmpty()) {
            refreshTable(id, null);
        } else {
            showAlert("Peringatan", "Masukkan ID Transaksi terlebih dahulu");
        }
    }

    private void handleReset() {
        idTransaksiField.clear();
        filterComboBox.getSelectionModel().selectFirst();
        refreshTable(null, null);
    }

    private void handleFilterChange() {
        String filterType = filterComboBox.getValue();
        if (!"Semua".equals(filterType)) {
            refreshTable(null, filterType);
        } else {
            refreshTable(null, null);
        }
    }

    
    private void loadPage(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat halaman: " + e.getMessage());
        }
    }

    private ParkingTransaction convertDocumentToTransaction(Document doc) {
        Double totalBayar = null;
        try {
            Object totalBayarObj = doc.get("total_bayar");
            if (totalBayarObj instanceof Double) {
                totalBayar = (Double) totalBayarObj;
            } else if (totalBayarObj instanceof Integer) {
                totalBayar = ((Integer) totalBayarObj).doubleValue();
            } else if (totalBayarObj instanceof String) {
                totalBayar = Double.parseDouble((String) totalBayarObj);
            }
        } catch (Exception e) {
            System.err.println("Error converting total_bayar: " + e.getMessage());
            totalBayar = 0.0;
        }
        
        return new ParkingTransaction(
            doc.getString("id_karcis"),
            doc.getString("jenis_kendaraan"),
            doc.getString("waktu_masuk"),
            doc.getString("waktu_keluar"),
            doc.getString("durasi"),
            totalBayar != null ? totalBayar : 0.0,
            doc.getString("tanggal_transaksi")
        );
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ParkingTransaction inner class
    public static class ParkingTransaction {
        private final IntegerProperty no = new SimpleIntegerProperty();
        private final StringProperty idKarcis = new SimpleStringProperty();
        private final StringProperty jenisKendaraan = new SimpleStringProperty();
        private final StringProperty waktuMasuk = new SimpleStringProperty();
        private final StringProperty waktuKeluar = new SimpleStringProperty();
        private final StringProperty durasi = new SimpleStringProperty();
        private final DoubleProperty totalBayar = new SimpleDoubleProperty();
        private final StringProperty tanggalTransaksi = new SimpleStringProperty();

        public ParkingTransaction(String idKarcis, String jenisKendaraan, String waktuMasuk,
                                String waktuKeluar, String durasi, Double totalBayar,
                                String tanggalTransaksi) {
            setIdKarcis(idKarcis);
            setJenisKendaraan(jenisKendaraan);
            setWaktuMasuk(waktuMasuk);
            setWaktuKeluar(waktuKeluar);
            setDurasi(durasi);
            setTotalBayar(totalBayar);
            setTanggalTransaksi(tanggalTransaksi);
        }

        // Getter and setter methods
        public int getNo() { return no.get(); }
        public void setNo(int value) { no.set(value); }
        public IntegerProperty noProperty() { return no; }

        public String getIdKarcis() { return idKarcis.get(); }
        public void setIdKarcis(String value) { idKarcis.set(value); }
        public StringProperty idKarcisProperty() { return idKarcis; }

        public String getJenisKendaraan() { return jenisKendaraan.get(); }
        public void setJenisKendaraan(String value) { jenisKendaraan.set(value); }
        public StringProperty jenisKendaraanProperty() { return jenisKendaraan; }

        public String getWaktuMasuk() { return waktuMasuk.get(); }
        public void setWaktuMasuk(String value) { waktuMasuk.set(value); }
        public StringProperty waktuMasukProperty() { return waktuMasuk; }

        public String getWaktuKeluar() { return waktuKeluar.get(); }
        public void setWaktuKeluar(String value) { waktuKeluar.set(value); }
        public StringProperty waktuKeluarProperty() { return waktuKeluar; }

        public String getDurasi() { return durasi.get(); }
        public void setDurasi(String value) { durasi.set(value); }
        public StringProperty durasiProperty() { return durasi; }

        public double getTotalBayar() { return totalBayar.get(); }
        public void setTotalBayar(double value) { totalBayar.set(value); }
        public DoubleProperty totalBayarProperty() { return totalBayar; }

        public String getTanggalTransaksi() { return tanggalTransaksi.get(); }
        public void setTanggalTransaksi(String value) { tanggalTransaksi.set(value); }
        public StringProperty tanggalTransaksiProperty() { return tanggalTransaksi; }
    }
}