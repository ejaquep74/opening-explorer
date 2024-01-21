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

        createHeaderRow(sheet);

        int rowCount = 1;
        for (GoodMove move : goodMoves) {
            Row row = sheet.createRow(rowCount++);
            writeGoodMove(move, row);
        }

        try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
            workbook.write(outputStream);
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);

        headerRow.createCell(0).setCellValue("FEN");
        headerRow.createCell(1).setCellValue("Move");
        headerRow.createCell(2).setCellValue("Probability Occurring");
        headerRow.createCell(3).setCellValue("Rating Rank");
        headerRow.createCell(4).setCellValue("Rating Percentile");
        headerRow.createCell(5).setCellValue("Average Rating For All Moves");
        headerRow.createCell(6).setCellValue("Average Rating");
        headerRow.createCell(7).setCellValue("Average Rating Opponents");
        headerRow.createCell(8).setCellValue("White Points Pct");
        // FIXME: add performance when it is well calculated:  headerRow.createCell(9).setCellValue("Performance");
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
        cell.setCellValue(move.getPerformance());

    }
}
