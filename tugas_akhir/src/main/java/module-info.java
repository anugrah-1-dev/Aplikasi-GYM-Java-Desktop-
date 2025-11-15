module sistem.parkir {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.graphics;

    // Java Standard
    requires java.sql;
    requires java.desktop;
    requires java.logging;
    requires jdk.jsobject;

    // Apache POI
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.scratchpad;
    requires org.apache.commons.collections4;
    
    // MongoDB
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
    requires org.mongodb.driver.sync.client;

    // JSON & Security
    requires com.google.gson;
    requires org.json;
    requires jbcrypt;

    // PDF & Reporting
    requires org.apache.pdfbox;
    requires jasperreports;
    requires itext;
    requires java.xml;

    // Barcode & QR
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires barcode4j;

    // Webcam
    requires webcam.capture;

    // Servlet & Web
    requires spark.core;

    // Logging
    requires org.slf4j;
    requires org.slf4j.simple;

    // Apache Commons
    requires org.apache.commons.lang3;

    // Open for reflection
    opens org.openjfx to javafx.fxml, javafx.web, javafx.graphics;
    
    exports org.openjfx;
}