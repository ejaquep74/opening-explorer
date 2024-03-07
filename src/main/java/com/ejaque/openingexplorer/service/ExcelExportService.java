package com.ejaque.openingexplorer.service;

import com.ejaque.openingexplorer.model.GoodMove;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    @Value("${output.file.path}")
    private String excelFilePath;

    public void generateExcel(List<GoodMove> goodMoves) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Good Moves");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        createHeaderRow(sheet, headerStyle);

        // Set column widths  
        sheet.setColumnWidth(0, 72 * 256); // 72 times the standard width
        int[] wideColumns = {2, 3, 4, 5, 6, 7, 8, 9, 10}; // C, D, E, F, G, H, I, J, K
        for (int colIndex : wideColumns) {
            sheet.setColumnWidth(colIndex, 16 * 256); // 16 times the standard width
        }

        int rowCount = 1;
        for (GoodMove move : goodMoves) {
            Row row = sheet.createRow(rowCount++);
            writeGoodMove(move, row);
        }

        try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
            workbook.write(outputStream);
        }
    }

    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);

        String[] headers = {"FEN", "Move", "Probability Occurring", "Rating Rank", 
                            "Rating Percentile", "Average Rating For All Moves", 
                            "Average Rating", "Average Rating Opponents", 
                            "White Points Pct", "Games Position", "Games Move", 
                            "Popularity%", "Ratio", "Eval"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeGoodMove(GoodMove move, Row row) {
        Cell cell = row.createCell(0);
        cell.setCellValue(move.getFen());

        cell = row.createCell(1);
        cell.setCellValue(move.getMove());

        cell = row.createCell(2);
        cell.setCellValue(move.getProbabilityOcurring());

        cell = row.createCell(3);
        cell.setCellValue(move.getRatingRank());

        cell = row.createCell(4);
        cell.setCellValue(move.getRatingPercentile());

        cell = row.createCell(5);
        cell.setCellValue(move.getAverageRatingForAllMoves());

        cell = row.createCell(6);
        cell.setCellValue(move.getAverageRating());

        cell = row.createCell(7);
        cell.setCellValue(move.getAverageRatingOpponents());

        cell = row.createCell(8);
        cell.setCellValue(move.getWhitePointsPctg());

        cell = row.createCell(9);
        cell.setCellValue(move.getTotalGames());

        cell = row.createCell(10);
        cell.setCellValue(move.getTotalGamesMove());

        cell = row.createCell(11);
        cell.setCellValue(move.getPopularity());

        // Calculate and set the Ratio value
        double averageRatingForAllMoves = move.getAverageRatingForAllMoves();
        double averageRating = move.getAverageRating();

        cell = row.createCell(12); // Ratio column
        if (averageRatingForAllMoves != 0) {
            double ratio = averageRating / averageRatingForAllMoves;
            cell.setCellValue(ratio);
        } else {
            cell.setCellValue("N/A"); // Or some other placeholder if division by zero
        }
        
        cell = row.createCell(13);
        cell.setCellValue(move.getEvaluation());
    }
}
