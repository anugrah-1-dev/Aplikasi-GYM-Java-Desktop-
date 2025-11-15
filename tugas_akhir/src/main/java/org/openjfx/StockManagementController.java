package org.openjfx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class StockManagementController implements Initializable {

    @FXML private Label clockLabel;
    @FXML private TableView<SnackController.SnackProduct> productTable;
    @FXML private TableColumn<SnackController.SnackProduct, String> colProductId;
    @FXML private TableColumn<SnackController.SnackProduct, String> colProductName;
    @FXML private TableColumn<SnackController.SnackProduct, String> colCategory;
    @FXML private TableColumn<SnackController.SnackProduct, Double> colPrice;
    @FXML private TableColumn<SnackController.SnackProduct, Integer> colStock;
    @FXML private TableColumn<SnackController.SnackProduct, String> colDescription;
    @FXML private TableColumn<SnackController.SnackProduct, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button addProductButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private SnackController.MongoDBService mongoDBService;
    private ObservableList<SnackController.SnackProduct> productList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mongoDBService = SnackController.MongoDBService.getInstance();
        productList = FXCollections.observableArrayList();

        setupTableColumns();
        setupCategoryFilter();
        setupEventHandlers();
        initializeClock();
        loadProducts();
    }

    private void setupTableColumns() {
        colProductId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Format kolom harga
        colPrice.setCellFactory(column -> new TableCell<SnackController.SnackProduct, Double>() {
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

        // Format kolom stok dengan warna
        colStock.setCellFactory(column -> new TableCell<SnackController.SnackProduct, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    if (item < 10) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (item < 30) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Setup action buttons column
        setupActionButtons();

        productTable.setItems(productList);
    }

    private void setupActionButtons() {
        Callback<TableColumn<SnackController.SnackProduct, Void>, TableCell<SnackController.SnackProduct, Void>> cellFactory =
            new Callback<>() {
                @Override
                public TableCell<SnackController.SnackProduct, Void> call(final TableColumn<SnackController.SnackProduct, Void> param) {
                    final TableCell<SnackController.SnackProduct, Void> cell = new TableCell<>() {
                        private final Button editBtn = new Button("Edit");
                        private final Button addStockBtn = new Button("+Stok");
                        private final Button deleteBtn = new Button("Hapus");

                        {
                            editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5px 10px; -fx-cursor: hand;");
                            addStockBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5px 10px; -fx-cursor: hand;");
                            deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 5px 10px; -fx-cursor: hand;");

                            editBtn.setOnAction(event -> {
                                SnackController.SnackProduct product = getTableView().getItems().get(getIndex());
                                editProduct(product);
                            });

                            addStockBtn.setOnAction(event -> {
                                SnackController.SnackProduct product = getTableView().getItems().get(getIndex());
                                addStock(product);
                            });

                            deleteBtn.setOnAction(event -> {
                                SnackController.SnackProduct product = getTableView().getItems().get(getIndex());
                                deleteProduct(product);
                            });
                        }

                        @Override
                        public void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                HBox buttons = new HBox(5, editBtn, addStockBtn, deleteBtn);
                                setGraphic(buttons);
                            }
                        }
                    };
                    return cell;
                }
            };

        colActions.setCellFactory(cellFactory);
    }

    private void setupCategoryFilter() {
        categoryFilter.setItems(FXCollections.observableArrayList(
            "Semua Kategori", "Protein", "Beverage", "Snack", "Supplement", "Air Mineral"
        ));
        categoryFilter.setValue("Semua Kategori");
        
        categoryFilter.setOnAction(e -> filterProducts());
    }

    private void setupEventHandlers() {
        addProductButton.setOnAction(e -> addNewProduct());
        refreshButton.setOnAction(e -> loadProducts());
        backButton.setOnAction(e -> goBack());
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterProducts());
    }

    private void initializeClock() {
        javafx.animation.Timeline clock = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, e -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                clockLabel.setText(LocalDateTime.now().format(formatter));
            }),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1))
        );
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
    }

    private void loadProducts() {
        try {
            List<SnackController.SnackProduct> products = mongoDBService.getAllProducts();
            productList.clear();
            productList.addAll(products);
            System.out.println("Loaded " + products.size() + " products");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat data produk: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();

        List<SnackController.SnackProduct> allProducts = mongoDBService.getAllProducts();
        productList.clear();

        for (SnackController.SnackProduct product : allProducts) {
            boolean matchSearch = searchText.isEmpty() || 
                                product.getProductName().toLowerCase().contains(searchText) ||
                                product.getProductId().toLowerCase().contains(searchText);
            
            boolean matchCategory = category.equals("Semua Kategori") || 
                                  product.getCategory().equals(category);

            if (matchSearch && matchCategory) {
                productList.add(product);
            }
        }
    }

    private void addNewProduct() {
        Dialog<SnackController.SnackProduct> dialog = new Dialog<>();
        dialog.setTitle("Tambah Produk Baru");
        dialog.setHeaderText("Masukkan detail produk baru");

        ButtonType saveButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField productIdField = new TextField();
        productIdField.setPromptText("SNK-XXX atau MINUMAN-XXX");
        TextField productNameField = new TextField();
        productNameField.setPromptText("Nama Produk");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(
            "Protein", "Beverage", "Snack", "Supplement", "Air Mineral"
        ));
        categoryCombo.setValue("Beverage");
        TextField priceField = new TextField();
        priceField.setPromptText("Harga");
        TextField stockField = new TextField();
        stockField.setPromptText("Stok Awal");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Deskripsi");
        TextField imageUrlField = new TextField();
        imageUrlField.setPromptText("/org/icon/nama-file.jpg");

        grid.add(new Label("ID Produk:"), 0, 0);
        grid.add(productIdField, 1, 0);
        grid.add(new Label("Nama Produk:"), 0, 1);
        grid.add(productNameField, 1, 1);
        grid.add(new Label("Kategori:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Harga:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Stok:"), 0, 4);
        grid.add(stockField, 1, 4);
        grid.add(new Label("Deskripsi:"), 0, 5);
        grid.add(descriptionField, 1, 5);
        grid.add(new Label("URL Gambar:"), 0, 6);
        grid.add(imageUrlField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        javafx.application.Platform.runLater(productIdField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    SnackController.SnackProduct product = new SnackController.SnackProduct();
                    product.setProductId(productIdField.getText());
                    product.setProductName(productNameField.getText());
                    product.setCategory(categoryCombo.getValue());
                    product.setPrice(Double.parseDouble(priceField.getText()));
                    product.setStock(Integer.parseInt(stockField.getText()));
                    product.setDescription(descriptionField.getText());
                    product.setActive(true);
                    return product;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Format harga atau stok tidak valid!");
                    return null;
                }
            }
            return null;
        });

        Optional<SnackController.SnackProduct> result = dialog.showAndWait();
        result.ifPresent(product -> {
            if (product.getProductId().isEmpty() || product.getProductName().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "ID Produk dan Nama Produk harus diisi!");
                return;
            }

            boolean success = mongoDBService.addProduct(product);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk berhasil ditambahkan!");
                loadProducts();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menambahkan produk!");
            }
        });
    }

    private void editProduct(SnackController.SnackProduct product) {
        Dialog<SnackController.SnackProduct> dialog = new Dialog<>();
        dialog.setTitle("Edit Produk");
        dialog.setHeaderText("Edit detail produk: " + product.getProductName());

        ButtonType saveButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField productNameField = new TextField(product.getProductName());
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(
            "Protein", "Beverage", "Snack", "Supplement", "Air Mineral"
        ));
        categoryCombo.setValue(product.getCategory());
        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        TextField stockField = new TextField(String.valueOf(product.getStock()));
        TextField descriptionField = new TextField(product.getDescription());

        grid.add(new Label("ID Produk:"), 0, 0);
        grid.add(new Label(product.getProductId()), 1, 0);
        grid.add(new Label("Nama Produk:"), 0, 1);
        grid.add(productNameField, 1, 1);
        grid.add(new Label("Kategori:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Harga:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Stok:"), 0, 4);
        grid.add(stockField, 1, 4);
        grid.add(new Label("Deskripsi:"), 0, 5);
        grid.add(descriptionField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    product.setProductName(productNameField.getText());
                    product.setCategory(categoryCombo.getValue());
                    product.setPrice(Double.parseDouble(priceField.getText()));
                    product.setStock(Integer.parseInt(stockField.getText()));
                    product.setDescription(descriptionField.getText());
                    product.setUpdatedAt(LocalDateTime.now());
                    return product;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Format harga atau stok tidak valid!");
                    return null;
                }
            }
            return null;
        });

        Optional<SnackController.SnackProduct> result = dialog.showAndWait();
        result.ifPresent(updatedProduct -> {
            boolean success = mongoDBService.updateProduct(updatedProduct);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk berhasil diupdate!");
                loadProducts();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal mengupdate produk!");
            }
        });
    }

    private void addStock(SnackController.SnackProduct product) {
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Tambah Stok");
        dialog.setHeaderText("Tambah stok untuk: " + product.getProductName());
        dialog.setContentText("Jumlah stok yang ditambahkan:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(stockStr -> {
            try {
                int additionalStock = Integer.parseInt(stockStr);
                if (additionalStock <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Jumlah stok harus lebih dari 0!");
                    return;
                }

                int newStock = product.getStock() + additionalStock;
                boolean success = mongoDBService.setStock(product.getProductId(), newStock);
                
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", 
                        "Stok berhasil ditambahkan!\n" +
                        "Stok lama: " + product.getStock() + "\n" +
                        "Stok baru: " + newStock);
                    loadProducts();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Gagal menambahkan stok!");
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Format stok tidak valid!");
            }
        });
    }

    private void deleteProduct(SnackController.SnackProduct product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Produk");
        alert.setContentText("Apakah Anda yakin ingin menghapus produk:\n" + 
                           product.getProductName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = mongoDBService.deleteProduct(product.getProductId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk berhasil dihapus!");
                loadProducts();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menghapus produk!");
            }
        }
    }

    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/menu_transaksi/menu_transaksi.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Menu Transaksi");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal kembali ke menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}