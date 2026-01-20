package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FantasyPlayerService {

    private final FantasyPlayerRepository fantasyPlayerRepository;

    @Transactional
    public void updatePlayer(Long seq, FantasyPlayerDto dto) {
        FantasyPlayer player = fantasyPlayerRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("Player not found with seq: " + seq));

        FantasyPlayer updated = FantasyPlayer.builder()
                .seq(player.getSeq())
                .name(dto.getName() != null ? dto.getName() : player.getName())
                .position(dto.getPosition() != null ? dto.getPosition() : player.getPosition())
                .team(dto.getTeam() != null ? dto.getTeam() : player.getTeam())
                .stats(dto.getStats() != null ? dto.getStats() : player.getStats())
                .build();

        fantasyPlayerRepository.save(updated);
    }

    @Transactional
    public void importPlayers(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            List<FantasyPlayer> players = new ArrayList<>();

            // Skip header row (index 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Assuming columns: Seq(0), Name(1), Position(2), Team(3), Stats(4)
                String name = getCellValueAsString(row.getCell(1));
                String position = getCellValueAsString(row.getCell(2));
                String team = getCellValueAsString(row.getCell(3));
                String stats = getCellValueAsString(row.getCell(4));

                // If name is empty, skip (or handle as end of data)
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }

                FantasyPlayer player = FantasyPlayer.builder()
                        .name(name)
                        .position(position)
                        .team(team)
                        .stats(stats)
                        .build();

                players.add(player);
            }

            // Delete existing and save new
            fantasyPlayerRepository.deleteAll();
            fantasyPlayerRepository.saveAll(players);

        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new RuntimeException("Failed to process Excel file", e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Check if it's an integer to avoid ".0"
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.format("%d", (long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
