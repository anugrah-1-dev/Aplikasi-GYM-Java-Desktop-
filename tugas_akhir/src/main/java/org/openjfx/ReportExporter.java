package org.openjfx;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ReportExporter {

    public static <S> void exportToPdf(TableView<S> table, String fileName) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            try {
                // Set margins and initial positions
                float margin = 40;
                float yStart = page.getMediaBox().getHeight() - margin;
                float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
                float yPosition = yStart;
                float bottomMargin = 70;
                float rowHeight = 20;
                float headerExtraHeight = 10;
                
                // Calculate column widths based on content
                int colCount = table.getColumns().size();
                float[] colWidths = new float[colCount];
                float totalWidth = 0;
                
                // Calculate minimum width for each column based on header and content
                for (int i = 0; i < colCount; i++) {
                    TableColumn<S, ?> column = table.getColumns().get(i);
                    String header = column.getText();
                    float headerWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(header) / 1000f * 12; // Font size 12
                    
                    float maxContentWidth = 0;
                    for (S item : table.getItems()) {
                        Object cellData = column.getCellData(item);
                        String text = (cellData != null) ? cellData.toString() : "";
                        float textWidth = PDType1Font.HELVETICA.getStringWidth(text) / 1000f * 10; // Font size 10
                        if (textWidth > maxContentWidth) {
                            maxContentWidth = textWidth;
                        }
                    }
                    
                    colWidths[i] = Math.max(headerWidth, maxContentWidth) + 10; // Add padding
                    totalWidth += colWidths[i];
                }
                
                // Adjust column widths if total exceeds table width
                if (totalWidth > tableWidth) {
                    float ratio = tableWidth / totalWidth;
                    for (int i = 0; i < colCount; i++) {
                        colWidths[i] *= ratio;
                    }
                }
                
                // Draw title
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Laporan Transaksi Parkir");
                contentStream.endText();
                yPosition -= 30;
                
                // Draw table headers with background
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float xPosition = margin;
                
                // Draw header background
                contentStream.setNonStrokingColor(200, 200, 200);
                contentStream.addRect(xPosition, yPosition - headerExtraHeight, tableWidth, rowHeight + headerExtraHeight);
                contentStream.fill();
                contentStream.setNonStrokingColor(0, 0, 0);
                
                // Draw header text
                int colIndex = 0;
                for (TableColumn<S, ?> column : table.getColumns()) {
                    String text = column.getText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition + 5, yPosition);
                    contentStream.showText(text);
                    contentStream.endText();
                    
                    // Draw vertical line
                    contentStream.moveTo(xPosition + colWidths[colIndex], yPosition - headerExtraHeight);
                    contentStream.lineTo(xPosition + colWidths[colIndex], yPosition + rowHeight);
                    contentStream.stroke();
                    
                    xPosition += colWidths[colIndex];
                    colIndex++;
                }
                
                // Draw horizontal line below header
                contentStream.moveTo(margin, yPosition - headerExtraHeight);
                contentStream.lineTo(margin + tableWidth, yPosition - headerExtraHeight);
                contentStream.stroke();
                
                yPosition -= (rowHeight + headerExtraHeight);
                
                // Draw table rows
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                ObservableList<S> items = table.getItems();
                
                for (int i = 0; i < items.size(); i++) {
                    S item = items.get(i);
                    xPosition = margin;
                    colIndex = 0;
                    
                    // Check if we need a new page
                    if (yPosition < bottomMargin) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        
                        // Reset positions for new page
                        yPosition = yStart - 30; // Account for title space
                        xPosition = margin;
                        
                        // Draw header again
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        contentStream.setNonStrokingColor(200, 200, 200);
                        contentStream.addRect(xPosition, yPosition - headerExtraHeight, tableWidth, rowHeight + headerExtraHeight);
                        contentStream.fill();
                        contentStream.setNonStrokingColor(0, 0, 0);
                        
                        colIndex = 0;
                        for (TableColumn<S, ?> column : table.getColumns()) {
                            String text = column.getText();
                            
                            contentStream.beginText();
                            contentStream.newLineAtOffset(xPosition + 5, yPosition);
                            contentStream.showText(text);
                            contentStream.endText();
                            
                            contentStream.moveTo(xPosition + colWidths[colIndex], yPosition - headerExtraHeight);
                            contentStream.lineTo(xPosition + colWidths[colIndex], yPosition + rowHeight);
                            contentStream.stroke();
                            
                            xPosition += colWidths[colIndex];
                            colIndex++;
                        }
                        
                        contentStream.moveTo(margin, yPosition - headerExtraHeight);
                        contentStream.lineTo(margin + tableWidth, yPosition - headerExtraHeight);
                        contentStream.stroke();
                        
                        yPosition -= (rowHeight + headerExtraHeight);
                        contentStream.setFont(PDType1Font.HELVETICA, 10);
                    }
                    
                    // Draw row data
                    xPosition = margin;
                    colIndex = 0;
                    for (TableColumn<S, ?> column : table.getColumns()) {
                        Object cellData = column.getCellData(item);
                        String text = (cellData != null) ? cellData.toString() : "";
                        
                        contentStream.beginText();
                        contentStream.newLineAtOffset(xPosition + 5, yPosition);
                        contentStream.showText(text);
                        contentStream.endText();
                        
                        // Draw vertical line
                        contentStream.moveTo(xPosition + colWidths[colIndex], yPosition);
                        contentStream.lineTo(xPosition + colWidths[colIndex], yPosition - rowHeight);
                        contentStream.stroke();
                        
                        xPosition += colWidths[colIndex];
                        colIndex++;
                    }
                    
                    // Draw horizontal line
                    contentStream.moveTo(margin, yPosition - rowHeight);
                    contentStream.lineTo(margin + tableWidth, yPosition - rowHeight);
                    contentStream.stroke();
                    
                    yPosition -= rowHeight;
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}