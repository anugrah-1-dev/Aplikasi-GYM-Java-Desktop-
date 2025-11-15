package org.openjfx;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class ParkingDataService {
    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "sistem_parkir";
    private static final String COLLECTION_NAME = "Transaksi";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<Document> fetchTransactions(String idTransaksi, String filterType, 
                                          LocalDate startDate, LocalDate endDate) {
        List<Document> documents = new ArrayList<>();
        
        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            Bson filter = buildFilter(idTransaksi, filterType, startDate, endDate);
            
            if (filter != null) {
                collection.find(filter).into(documents);
            } else {
                collection.find().into(documents);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return documents;
    }

    private Bson buildFilter(String idTransaksi, String filterType, LocalDate startDate, LocalDate endDate) {
        List<Bson> filters = new ArrayList<>();

         if (filterType != null && filterType.equals("TanggalSpesifik") && startDate != null) {
        String dateStr = startDate.format(DATE_FORMATTER);
        filters.add(Filters.regex("tanggal_transaksi", "^" + dateStr));
    }

    

        // Add ID filter if provided
        if (idTransaksi != null && !idTransaksi.isEmpty()) {
            filters.add(Filters.eq("id_karcis", idTransaksi));
        }

        // Handle date range filter
        if (startDate != null && endDate != null) {
            filters.add(Filters.and(
                Filters.gte("tanggal_transaksi", startDate.format(DATE_FORMATTER)),
                Filters.lte("tanggal_transaksi", endDate.format(DATE_FORMATTER))
            ));
        } 
        
        // Handle predefined filters (daily, weekly, monthly) if no custom date range is provided
        else if (filterType != null && !filterType.equals("Semua")) {
            LocalDate today = LocalDate.now();
            
            switch (filterType) {
                case "Harian":
                    String todayStr = today.format(DATE_FORMATTER);
                    filters.add(Filters.regex("tanggal_transaksi", "^" + todayStr));
                    break;
                    
                case "Mingguan":
                    LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    LocalDate endOfWeek = startOfWeek.plusDays(6);
                    filters.add(Filters.and(
                        Filters.gte("tanggal_transaksi", startOfWeek.format(DATE_FORMATTER)),
                        Filters.lte("tanggal_transaksi", endOfWeek.format(DATE_FORMATTER))
                    ));
                    break;
                    
                case "Bulanan":
                    LocalDate startOfMonth = today.withDayOfMonth(1);
                    LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                    filters.add(Filters.and(
                        Filters.gte("tanggal_transaksi", startOfMonth.format(DATE_FORMATTER)),
                        Filters.lte("tanggal_transaksi", endOfMonth.format(DATE_FORMATTER))
                    ));
                    break;
            }
        }
        
        // Combine all filters with AND
        return filters.isEmpty() ? null : Filters.and(filters);
    }
}