package org.openjfx;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DatabaseKarcis {
    private static final String URI = "mongodb://localhost:27017"; // Ganti sesuai dengan konfigurasi MongoDB
    private static final String DATABASE_NAME = "sistem_parkir"; // Nama database MongoDB

    public static MongoDatabase connect() {
        MongoClient mongoClient = null;
        try {
            mongoClient = MongoClients.create(URI);
            return mongoClient.getDatabase(DATABASE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Atau bisa throw exception sesuai kebutuhan
        } finally {
            if (mongoClient != null) {
                mongoClient.close(); // Pastikan untuk menutup koneksi
            }
        }
    }
}