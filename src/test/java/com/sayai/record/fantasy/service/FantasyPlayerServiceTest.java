package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FantasyPlayerServiceTest {

    @Mock
    private FantasyPlayerRepository fantasyPlayerRepository;

    @InjectMocks
    private FantasyPlayerService fantasyPlayerService;

    @Test
    void importPlayers_shouldParseAndSave() throws IOException {
        // Create a dummy Excel file in memory
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Players");

        // Header
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Seq");
        header.createCell(1).setCellValue("Name");
        header.createCell(2).setCellValue("Position");
        header.createCell(3).setCellValue("Team");
        header.createCell(4).setCellValue("Stats");

        // Data Row 1
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue(1);
        row1.createCell(1).setCellValue("Player One");
        row1.createCell(2).setCellValue("P");
        row1.createCell(3).setCellValue("Team A");
        row1.createCell(4).setCellValue("10 Wins");

        // Data Row 2
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue(2);
        row2.createCell(1).setCellValue("Player Two");
        row2.createCell(2).setCellValue("C");
        row2.createCell(3).setCellValue("Team B");
        row2.createCell(4).setCellValue("0.300 AVG");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] content = bos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        );

        // Execute
        fantasyPlayerService.importPlayers(file);

        // Verify
        verify(fantasyPlayerRepository).deleteAll();
        verify(fantasyPlayerRepository).saveAll(anyList());
    }
}
