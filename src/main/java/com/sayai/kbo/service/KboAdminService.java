package com.sayai.kbo.service;

import com.sayai.kbo.dto.KboGameUploadRequest;
import com.sayai.kbo.dto.KboGameUploadResponse;
import com.sayai.kbo.model.*;
import com.sayai.kbo.repository.*;
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
import java.util.ArrayList;
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
    private final KboHitterStatsRepository kboHitterStatsRepository;
    private final KboPitcherStatsRepository kboPitcherStatsRepository;
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

    @Transactional(readOnly = true)
    public KboGameUploadResponse getGameDetails(Long gameIdx) {
        KboGame game = kboGameRepository.findById(gameIdx).orElseThrow(() -> new IllegalArgumentException("Game not found"));
        List<KboHit> hitters = kboHitRepository.findByGameIdx(gameIdx);
        List<KboPitch> pitchers = kboPitchRepository.findByGameIdx(gameIdx);
        return KboGameUploadResponse.builder()
                .game(game)
                .hitters(hitters)
                .pitchers(pitchers)
                .build();
    }

    @Transactional
    public KboGameUploadResponse uploadGame(KboGameUploadRequest request) throws Exception {

        String dateStr = request.getGameTime() != null ? request.getGameTime().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "00000000";
        String timeStr = request.getGameTime() != null ? request.getGameTime().format(DateTimeFormatter.ofPattern("HH")) : "00";
        String awayCode = getTeamCode(request.getAway());
        String homeCode = getTeamCode(request.getHome());

        String gameIdxStr = dateStr + timeStr + awayCode + homeCode;
        Long newGameId = Long.parseLong(gameIdxStr);

        // Delete existing game if it exists to support overwriting
        if (kboGameRepository.existsById(newGameId)) {
            kboGameRepository.deleteById(newGameId);
            kboGameRepository.flush(); // Ensure deletion is cascaded and committed before creating a new one
        }

        String resultTeam = null;
        if (request.getHomeScore() > request.getAwayScore()) {
            resultTeam = request.getHome();
        } else if (request.getHomeScore() < request.getAwayScore()) {
            resultTeam = request.getAway();
        }

        KboGame game = KboGame.builder()
                .id(newGameId)
                .season(request.getSeason())
                .home(request.getHome())
                .away(request.getAway())
                .homeScore(request.getHomeScore())
                .awayScore(request.getAwayScore())
                .result(resultTeam)
                .build();

        game = kboGameRepository.save(game);

        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        List<KboHit> allHitters = new ArrayList<>();
        List<KboPitch> allPitchers = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Sheet 0: Away team
            Sheet awaySheet = workbook.getSheetAt(0);
            processSheet(awaySheet, game, request.getAway(), allHitters, allPitchers);

            // Sheet 1: Home team
            if (workbook.getNumberOfSheets() > 1) {
                Sheet homeSheet = workbook.getSheetAt(1);
                processSheet(homeSheet, game, request.getHome(), allHitters, allPitchers);
            }
        }

        int season = (int) (newGameId / 10_000_000_000L);
        updateHitterStats(allHitters, season);
        updatePitcherStats(allPitchers, season);

        return KboGameUploadResponse.builder()
                .game(game)
                .hitters(allHitters)
                .pitchers(allPitchers)
                .build();
    }

    private void updateHitterStats(List<KboHit> hitters, int season) {
        long startIdx = (long) season * 10_000_000_000L;
        long endIdx = (long) (season + 1) * 10_000_000_000L - 1;

        for (KboHit hit : hitters) {
            Long playerId = hit.getPlayerId();
            Optional<KboHitterStats> existing = kboHitterStatsRepository.findByPlayerIdAndSeason(playerId, season);

            if (existing.isPresent()) {
                KboHitterStats stats = existing.get();
                int newAb = stats.getAb() + hit.getAb().intValue();
                int newPa = stats.getPa() + hit.getPa().intValue();
                int newHit = stats.getHit() + hit.getHit().intValue();
                int newHr = stats.getHr() + hit.getHr().intValue();
                int newRbi = stats.getRbi() + hit.getRbi().intValue();
                int newSo = stats.getSo() + hit.getSo().intValue();
                int newSb = stats.getSb() + hit.getSb().intValue();
                String newAvg = calcAvg(newHit, newPa);

                kboHitterStatsRepository.save(KboHitterStats.builder()
                        .playerId(playerId)
                        .season(season)
                        .ab(newAb).pa(newPa).hit(newHit).hr(newHr)
                        .rbi(newRbi).so(newSo).sb(newSb).avg(newAvg)
                        .build());
            } else {
                KboHitterSeasonStatInterface seasonStats = kboHitRepository.getSeasonStatsByPlayerId(playerId, startIdx, endIdx);
                int totalAb = seasonStats != null ? seasonStats.getAb().intValue() : 0;
                int totalPa = seasonStats != null ? seasonStats.getPa().intValue() : 0;
                int totalHit = seasonStats != null ? seasonStats.getHit().intValue() : 0;
                int totalHr = seasonStats != null ? seasonStats.getHr().intValue() : 0;
                int totalRbi = seasonStats != null ? seasonStats.getRbi().intValue() : 0;
                int totalSo = seasonStats != null ? seasonStats.getSo().intValue() : 0;
                int totalSb = seasonStats != null ? seasonStats.getSb().intValue() : 0;
                String avgStr = calcAvg(totalHit, totalAb);

                kboHitterStatsRepository.save(KboHitterStats.builder()
                        .playerId(playerId)
                        .season(season)
                        .ab(totalAb).pa(totalPa).hit(totalHit).hr(totalHr)
                        .rbi(totalRbi).so(totalSo).sb(totalSb).avg(avgStr)
                        .build());
            }
        }
    }

    private void updatePitcherStats(List<KboPitch> pitchers, int season) {
        long startIdx = (long) season * 10_000_000_000L;
        long endIdx = (long) (season + 1) * 10_000_000_000L - 1;

        for (KboPitch pitch : pitchers) {
            Long playerId = pitch.getPlayerId();
            Optional<KboPitcherStats> existing = kboPitcherStatsRepository.findByPlayerIdAndSeason(playerId, season);

            if (existing.isPresent()) {
                KboPitcherStats stats = existing.get();
                int newOuts = stats.getOuts() + (int) (pitch.getInning() * 3);
                int newEr = stats.getEr() + pitch.getEr().intValue();
                int newWin = stats.getWin() + pitch.getWin().intValue();
                int newSo = stats.getSo() + pitch.getSo().intValue();
                int newSave = stats.getSave() + pitch.getSave().intValue();
                int newBb = stats.getBb() + pitch.getBb().intValue();
                int newPhit = stats.getPhit() + pitch.getHit().intValue();
                String newEra = calcEra(newEr, newOuts);
                String newWhip = calcWhip(newBb, newPhit, newOuts);

                kboPitcherStatsRepository.save(KboPitcherStats.builder()
                        .playerId(playerId)
                        .season(season)
                        .outs(newOuts).er(newEr).win(newWin).so(newSo)
                        .save(newSave).bb(newBb).phit(newPhit)
                        .era(newEra).whip(newWhip)
                        .build());
            } else {
                KboPitcherSeasonStatInterface seasonStats = kboPitchRepository.getSeasonStatsByPlayerId(playerId, startIdx, endIdx);
                int totalOuts = seasonStats != null ? seasonStats.getOuts().intValue() : 0;
                int totalEr = seasonStats != null ? seasonStats.getEr().intValue() : 0;
                int totalWin = seasonStats != null ? seasonStats.getWin().intValue() : 0;
                int totalSo = seasonStats != null ? seasonStats.getSo().intValue() : 0;
                int totalSave = seasonStats != null ? seasonStats.getSave().intValue() : 0;
                int totalBb = seasonStats != null ? seasonStats.getBb().intValue() : 0;
                int totalPhit = seasonStats != null ? seasonStats.getPhit().intValue() : 0;
                String eraStr = calcEra(totalEr, totalOuts);
                String whipStr = calcWhip(totalBb, totalPhit, totalOuts);

                kboPitcherStatsRepository.save(KboPitcherStats.builder()
                        .playerId(playerId)
                        .season(season)
                        .outs(totalOuts).er(totalEr).win(totalWin).so(totalSo)
                        .save(totalSave).bb(totalBb).phit(totalPhit)
                        .era(eraStr).whip(whipStr)
                        .build());
            }
        }
    }

    private String calcAvg(int hit, int ab) {
        if (ab == 0) return "0.000";
        return String.format("%.3f", (double) hit / ab);
    }

    private String calcEra(int er, int outs) {
        if (outs == 0) return "0.00";
        return String.format("%.2f", (double) er / outs * 27);
    }

    private String calcWhip(int bb, int phit, int outs) {
        if (outs == 0) return "0.00";
        return String.format("%.2f", (double) (bb + phit) / outs * 3);
    }

    private void processSheet(Sheet sheet, KboGame game, String teamName, List<KboHit> hittersOut, List<KboPitch> pitchersOut) {
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
                long ab = getLongValueSafe(row, headerMap, "타수");
                long hit = getLongValueSafe(row, headerMap, "안타");
                long rbi = getLongValueSafe(row, headerMap, "타점");
                long run = getLongValueSafe(row, headerMap, "득점");
                long sb = getLongValueSafe(row, headerMap, "도루");

                // Calculate pa, so and hr from inning columns dynamically (e.g., '1', '2', '3'...)
                long pa = 0;
                long so = 0;
                long hr = 0;
                for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                    String headerName = entry.getKey();
                    if (headerName.matches("\\d+")) { // If header is a number (inning 1, 2, 3...)
                        String cellVal = getCellValueAsString(row.getCell(entry.getValue()));

                        // Count Plate Appearances (PA) in this inning
                        // Split by "/" or simply check non-empty
                        if (cellVal != null && !cellVal.trim().isEmpty() && !cellVal.trim().equals("-")) {
                            String[] atBats = cellVal.split("/");
                            for (String atBat : atBats) {
                                if (!atBat.trim().isEmpty() && !atBat.trim().equals("-")) {
                                    pa++;
                                }
                            }
                        }

                        if (cellVal.contains("삼진") || cellVal.contains("스낫")) {
                            so++;
                        }
                        if (cellVal.contains("홈")) {
                            hr++;
                        }
                    }
                }

                if (pa == 0 && sb == 0) continue; // "타수가 0 인데 도루가 0이 아닌경우 해당 선수의 데이터는 추가하는 로직을 넣어줘 (대주자 로직)"

                FantasyPlayer fp = findMatchingPlayer(teamPlayers, playerName);
                if (fp != null) {
                    KboHit kboHit = KboHit.builder()
                            .gameIdx(game.getId())
                            .playerId(fp.getSeq())
                            .pa(pa).ab(ab).hit(hit).rbi(rbi).run(run).sb(sb).so(so).hr(hr)
                            .build();
                    kboHit = kboHitRepository.save(kboHit);
                    hittersOut.add(kboHit);
                } else {
                    throw new IllegalArgumentException(teamName + " 팀의 " + playerName + " 선수가 등록되어 있지 않습니다. 선수 등록이 필요합니다.");
                }
            } else {
                // Parse Pitcher row
                long win = 0;
                long lose = 0;
                long save = 0;

                Integer resultColIdx = headerMap.get("결과");
                if (resultColIdx != null) {
                    String resultVal = getCellValueAsString(row.getCell(resultColIdx)).trim();
                    if (resultVal.contains("승")) {
                        win = 1;
                    } else if (resultVal.contains("패")) {
                        lose = 1;
                    } else if (resultVal.contains("세")) {
                        save = 1;
                    }
                }

                long inning = getLongValueSafe(row, headerMap, "이닝");

                long batter = getLongValueSafe(row, headerMap, "타자");
                long pitchCnt = getLongValueSafe(row, headerMap, "투구수");
                long hit = getLongValueSafe(row, headerMap, "피안타");
                long bb = getLongValueSafe(row, headerMap, "4사구");
                long so = getLongValueSafe(row, headerMap, "삼진");
                long er = getLongValueSafe(row, headerMap, "자책");
                long hbp = getLongValueSafe(row, headerMap, "사구");

                FantasyPlayer fp = findMatchingPlayer(teamPlayers, playerName);
                if (fp != null) {
                    KboPitch kboPitch = KboPitch.builder()
                            .gameIdx(game.getId())
                            .playerId(fp.getSeq())
                            .win(win).lose(lose).save(save).inning(inning).batter(batter)
                            .pitchCnt(pitchCnt).hit(hit).bb(bb).so(so).er(er).hbp(hbp)
                            .build();
                    kboPitch = kboPitchRepository.save(kboPitch);
                    pitchersOut.add(kboPitch);
                } else {
                    throw new IllegalArgumentException(teamName + " 팀의 " + playerName + " 선수가 등록되어 있지 않습니다. 선수 등록이 필요합니다.");
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

        val = val.replaceAll("[^0-9-]", "");
        if (val.isEmpty() || val.equals("-")) return 0L;

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

}
