package com.sayai.kbo.service;

import com.sayai.kbo.dto.KboGameUploadRequest;
import com.sayai.kbo.model.KboGame;
import com.sayai.kbo.model.KboHit;
import com.sayai.kbo.model.KboPitch;
import com.sayai.kbo.repository.KboGameRepository;
import com.sayai.kbo.repository.KboHitRepository;
import com.sayai.kbo.repository.KboPitchRepository;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KboAdminService {

    private final KboGameRepository kboGameRepository;
    private final KboHitRepository kboHitRepository;
    private final KboPitchRepository kboPitchRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;

    private String getTeamCode(String teamName) {
        if (teamName == null) return "99";
        return switch (teamName.toUpperCase()) {
            case "두산" -> "00";
            case "LG" -> "01";
            case "키움" -> "02";
            case "KT" -> "03";
            case "SSG" -> "04";
            case "한화" -> "05";
            case "KIA" -> "06";
            case "삼성" -> "07";
            case "NC" -> "08";
            case "롯데" -> "09";
            default -> "99";
        };
    }

    @Transactional
    public void uploadGame(KboGameUploadRequest request) throws Exception {

        String dateStr = request.getGameDate() != null ? request.getGameDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "00000000";
        String timeStr = request.getGameTime() != null ? request.getGameTime().format(DateTimeFormatter.ofPattern("HH")) : "00";
        String awayCode = getTeamCode(request.getAway());
        String homeCode = getTeamCode(request.getHome());

        String gameIdxStr = dateStr + timeStr + awayCode + homeCode;
        Long newGameId = Long.parseLong(gameIdxStr);

        KboGame game = KboGame.builder()
                .id(newGameId)
                .season(request.getSeason())
                .home(request.getHome())
                .away(request.getAway())
                .homeScore(request.getHomeScore())
                .awayScore(request.getAwayScore())
                .result(request.getResult())
                .build();

        kboGameRepository.save(game);

        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Sheet 0: Away team
            Sheet awaySheet = workbook.getSheetAt(0);
            processSheet(awaySheet, game, request.getAway());

            // Sheet 1: Home team
            if (workbook.getNumberOfSheets() > 1) {
                Sheet homeSheet = workbook.getSheetAt(1);
                processSheet(homeSheet, game, request.getHome());
            }
        }
    }

    private void processSheet(Sheet sheet, KboGame game, String teamName) {
        boolean readingHitters = true;

        List<FantasyPlayer> teamPlayers = fantasyPlayerRepository.findPlayers(teamName, null, null, null);

        Map<String, Integer> headerMap = new HashMap<>();

        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // Find headers
            String cVal = getCellValueAsString(row.getCell(2)); // C col usually has "선수명"
            if ("선수명".equals(cVal) || "선수명".equals(getCellValueAsString(row.getCell(0)))) {
                headerMap.clear();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String headerName = getCellValueAsString(row.getCell(c));
                    if (!headerName.isEmpty()) {
                        headerMap.put(headerName, c);
                    }
                }

                if (headerMap.containsKey("등판")) {
                    readingHitters = false;
                } else {
                    readingHitters = true;
                }
                continue;
            }

            if (headerMap.isEmpty()) continue;

            Integer nameIdx = headerMap.get("선수명");
            if (nameIdx == null) continue;

            String playerName = getCellValueAsString(row.getCell(nameIdx));
            if (playerName.isEmpty() || playerName.equals("선수명")) continue;

            if (readingHitters) {
                // Parse Hitter row
                long pa = getLongValueSafe(row, headerMap, "타석");
                if (pa == 0) continue; // "타석이 0 인 선수의 데이터는 넣을 필요 없어."

                long ab = getLongValueSafe(row, headerMap, "타수");
                long hit = getLongValueSafe(row, headerMap, "안타");
                long rbi = getLongValueSafe(row, headerMap, "타점");
                long run = getLongValueSafe(row, headerMap, "득점");
                long sb = getLongValueSafe(row, headerMap, "도루");

                // SO and HR are requested but not explicitly visible in image, if present read them
                long so = getLongValueSafe(row, headerMap, "삼진");
                long hr = getLongValueSafe(row, headerMap, "홈런");

                FantasyPlayer fp = findMatchingPlayer(teamPlayers, playerName);
                if (fp != null) {
                    KboHit kboHit = KboHit.builder()
                            .game(game)
                            .player(fp)
                            .pa(pa).ab(ab).hit(hit).rbi(rbi).run(run).sb(sb).so(so).hr(hr)
                            .build();
                    kboHitRepository.save(kboHit);
                }
            } else {
                // Parse Pitcher row
                long win = getLongValueSafe(row, headerMap, "승");
                long lose = getLongValueSafe(row, headerMap, "패");
                long save = getLongValueSafe(row, headerMap, "세");

                String inningStr = "";
                Integer innIdx = headerMap.get("이닝");
                if (innIdx != null) {
                    inningStr = getCellValueAsString(row.getCell(innIdx));
                }
                long inning = parseInning(inningStr);

                long batter = getLongValueSafe(row, headerMap, "타자");
                long pitchCnt = getLongValueSafe(row, headerMap, "투구수");
                long hit = getLongValueSafe(row, headerMap, "피안타");
                long bb = getLongValueSafe(row, headerMap, "4사구");
                long so = getLongValueSafe(row, headerMap, "삼진");
                long er = getLongValueSafe(row, headerMap, "자책");
                long hbp = getLongValueSafe(row, headerMap, "사구"); // if present

                FantasyPlayer fp = findMatchingPlayer(teamPlayers, playerName);
                if (fp != null) {
                    KboPitch kboPitch = KboPitch.builder()
                            .game(game)
                            .player(fp)
                            .win(win).lose(lose).save(save).inning(inning).batter(batter)
                            .pitchCnt(pitchCnt).hit(hit).bb(bb).so(so).er(er).hbp(hbp)
                            .build();
                    kboPitchRepository.save(kboPitch);
                }
            }
        }
    }

    private FantasyPlayer findMatchingPlayer(List<FantasyPlayer> players, String name) {
        return players.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> {
                    double val = cell.getNumericCellValue();
                    if (val == (long) val) {
                        yield String.valueOf((long) val);
                    } else {
                        yield String.valueOf(val);
                    }
                }
                case FORMULA -> {
                    try {
                        yield cell.getStringCellValue().trim();
                    } catch (Exception e) {
                        yield String.valueOf((long) cell.getNumericCellValue());
                    }
                }
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private long getLongValueSafe(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer colIdx = headerMap.get(headerName);
        if (colIdx == null) return 0L;

        Cell cell = row.getCell(colIdx);
        if (cell == null) return 0L;

        String val = getCellValueAsString(cell).trim();
        if (val.isEmpty() || val.equals("-") || val.equals(" ")) return 0L;

        // Extract numbers only, e.g., if it has text around it
        val = val.replaceAll("[^0-9-]", "");
        if (val.isEmpty() || val.equals("-")) return 0L;

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long parseInning(String inningStr) {
        if (inningStr == null || inningStr.isEmpty()) return 0;
        inningStr = inningStr.trim();
        long totalOuts = 0;
        String[] parts = inningStr.split(" ");
        for (String part : parts) {
            if (part.contains("/")) {
                String[] frac = part.split("/");
                try {
                    totalOuts += Long.parseLong(frac[0]);
                } catch (Exception ignore) {}
            } else {
                try {
                    // Extract numeric part (e.g. "5" -> 15 outs)
                    String num = part.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        totalOuts += Long.parseLong(num) * 3;
                    }
                } catch (Exception ignore) {}
            }
        }
        return totalOuts;
    }
}
