package org.openjfx;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DatabaseConnector {
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static void initialize() {
        try {
            // Sesuaikan dengan URI database Anda
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("gym");
            System.out.println("Connected to database: " + database.getName());
        } catch (Exception e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("Database connection closed.");
        }
    }

    static MongoCollection<Document> getHargaKarcisCollection() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}