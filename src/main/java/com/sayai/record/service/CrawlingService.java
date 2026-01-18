package com.sayai.record.service;

import com.sayai.record.dto.ResponseDto;
import com.sayai.record.model.*;
import com.sayai.record.model.enums.FirstLast;
import com.sayai.record.repository.*;
import com.sayai.record.util.CodeCache;
import com.sayai.record.util.ExpandArrayList;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlingService {
    private final PlayerService playerService;
    private final LigueService ligueService;
    private final HitService hitService;
    private final PitchService pitchService;
    private final GameService gameService;
    private final CodeCache codeCache;
    private final HitterBoardService hitterBoardService;
    private final PitcherBoardService pitcherBoardService;

    public ResponseDto crawl(String url){
        return this.crawl(url, Long.valueOf(LocalDate.now().getYear()));
    }
    @Transactional
    public ResponseDto crawl(String url, Long season){
        if (season==null)
            season = Long.valueOf(LocalDateTime.now().getYear());
        Connection conn = Jsoup.connect(url);
        Document document = null;
        try {
            document = conn.get();
            //url의 내용을 HTML Document 객체로 가져온다.
            //https://jsoup.org/apidocs/org/jsoup/nodes/Document.html 참고
        } catch (IOException e) {
            e.printStackTrace();
        }
        Long gameId = Long.parseLong(url.split("&")[1].split("=")[1]);
        log.info("GAMEID {}", gameId);
        Optional<Game> gameSearched = gameService.findGame(gameId);
        if(gameSearched.isPresent())
            return new ResponseDto().builder().resultMsg("Game Already Exists").resultCode(20001).build();
        String fl;
        Elements scorebox = document.getElementsByClass("section_score");
        String fT = scorebox.get(0).child(0).select("dt").text();
        String lT = scorebox.get(0).child(1).select("dt").text();
        String scores =scorebox.get(0).getElementsByClass("score").text();
        Long homeScore = Long.parseLong(scores.split(" ")[0]);
        Long awayScore = Long.parseLong(scores.split(" ")[1]);
        String opponent;
        String result = "패";
        if(homeScore > awayScore){
            if(fT.equals("팀 사야이"))
                result = "승";
        }else if(homeScore < awayScore){
            if(lT.equals("팀 사야이"))
                result = "승";
        }else{
            result = "무";
        }
        String ltp = scorebox.get(0).getElementsByClass("score_teble").select("caption").text();
        String league = ltp.split("/")[0].trim();
        String time = ltp.split("/")[1].trim();
        String place = ltp.split("/")[2].trim();
        if(ltp.split("/").length>3){
            int len = ltp.split("/").length;
            time = ltp.split("/")[len-2].trim();
            place = ltp.split("/")[len-1].trim();
            league = Arrays.stream(ltp.split("/"),0,len-2)
                    .collect(Collectors.joining("/"));
        }
        if(fT.equals("팀 사야이")){ fl = "F"; opponent = lT;}
        else {fl = "L"; opponent=fT;}

        Optional<Ligue> byName = ligueService.findByName(league, season);
        Long ligId = ligueService.findByName(league, season)
                .orElse(ligueService.findByName("원외리그", season).orElseThrow(NoSuchElementException::new))
                .getId();
        int time_MM = Integer.parseInt(time.substring(0,2));
        int time_dd = Integer.parseInt(time.substring(3,5));
        int time_hh = Integer.parseInt(time.substring(6,8));
        int time_mm = Integer.parseInt(time.substring(9,11));
        LocalDate gamedate = LocalDate.of(season.intValue(),time_MM,time_dd);
        LocalTime gametime = LocalTime.of(time_hh,time_mm);
        Game game = Game.builder().id(gameId)
                .clubId(15387L).fl(FirstLast.valueOf(fl)).stadium(place).gameDate(gamedate)
                .gameTime(gametime).season((long) gamedate.getYear()).leagueId(ligId).opponent(opponent)
                .homeScore(homeScore).awayScore(awayScore).result(result).build();
        Game saveGame = gameService.saveGame(game);


        List<Hit> hitList = new ArrayList<>();
        List<Pitch> pitchList = new ArrayList<>();
        List<HitterBoard> hitterBoardList = new ArrayList<>();
        List<PitcherBoard> pitcherBoardList = new ExpandArrayList<>();
        Elements record = document.getElementsByClass("record_table");
        Element hitcells;
        Element pitchcells;
        Element pitchboardcells;
        if(fl.equals("F")){
            hitcells = record.get(0);
            pitchboardcells = record.get(1);
            pitchcells = record.get(2);
        }else{
            pitchboardcells = record.get(0);
            hitcells = record.get(1);
            pitchcells = record.get(3);
        }
        Elements cells1= hitcells.select("table tbody tr td");
        Elements players1 = hitcells.select("table tbody tr th");
        int numRows1 = hitcells.select("table tbody tr").size();
        int numCols1 = hitcells.select("table tbody tr:first-child td").size();
        String[][] table1 = new String[numRows1][numCols1];
        String[] player1= new String[numRows1];
        int kk = 0;
        for(Element ele : players1){
            player1[kk++] = ele.text().split(" ")[0]+ " "+ele.text().split(" ")[2].substring(0,3);
        }
        int ii = 0, jj = 0;
        for (Element cell : cells1) {
            table1[ii][jj++] = cell.text();
            if (jj == numCols1) {
                ii++;
                jj = 0;
            }
        }
        String[][] hittable = new String[numRows1][numCols1+1];
        for(int i=0; i<numRows1; i++){
            hittable[i][0] = player1[i];
            for(int j=1; j<numCols1; j++){
                hittable[i][j] = table1[i][j-1];
            }
        }
        long gameseq = 1L;
        for(int i=0; i<numRows1; i++){
            String name = hittable[i][0].split(" ")[1];
            Player player = playerService.getPlayerByName(name).get();
            Long hitseq = 1L;
            for(int j=1; j<12; j++){
                if(!hittable[i][j].isEmpty()){
                    int cnt = hittable[i][j].split("/").length;
                    for(int k=0; k<cnt; k++){
                        String hc = hittable[i][j].split("/")[k];
                        if(codeCache.getData(hc) == null)
                            continue;
                        Hit hit = Hit.builder().gameSeq(gameseq).game(saveGame).player(player).inning((long)j)
                                .hitNo(Long.parseLong(hittable[i][0].split(" ")[0]))
                                .hitSeq(hitseq).hitCd(codeCache.getData(hc))
                                .result(hc)
                                .build();
                        hitList.add(hit);
                        hitseq++;
                        gameseq++;
                    }
                }
            }
            HitterBoard hitterBoard = HitterBoard.builder()
                    .player(player).game(saveGame)
                    .hitNo(Long.parseLong(hittable[i][0].split(" ")[0]))
                    .playerApp(Integer.parseInt(hittable[i][13]))
                    .hits(Integer.parseInt(hittable[i][14]))
                    .rbi(Integer.parseInt(hittable[i][15]))
                    .runs(Integer.parseInt(hittable[i][16]))
                    .sb(Integer.parseInt(hittable[i][17])).build();
            hitterBoardList.add(hitterBoard);
        }
        hitService.saveAll(hitList);
        hitterBoardService.saveAll(hitterBoardList);

        Elements cells2= pitchcells.select("table tbody tr td");
        Elements players2 = pitchcells.select("table tbody tr th");
        int numRows2 = pitchcells.select("table tbody tr").size();
        int numCols2 = pitchcells.select("table tbody tr:first-child td").size();
        String[][] table2 = new String[numRows2][numCols2];
        String[] player2= new String[numRows2];
        kk = 0;
        for(Element ele : players2){
            player2[kk++] = ele.text().substring(0,3);
        }
        ii = 0;
        jj = 0;
        for (Element cell : cells2) {
            table2[ii][jj++] = cell.text();
            if (jj == numCols2) {
                ii++;
                jj = 0;
            }
        }
        String[][] pitchtable = new String[numRows2][numCols2+1];
        for(int i=0; i<numRows2; i++){
            pitchtable[i][0] = player2[i];
            for(int j=1; j<numCols2; j++){
                pitchtable[i][j] = table2[i][j-1];
            }
        }
        for(int i=0; i<numRows2; i++){
            String name = pitchtable[i][0];
            String innStr = pitchtable[i][2];
            Long inn = 0L;
            if(innStr.length()>=3){
                if(innStr.substring(2).equals("⅔")){
                    inn = Long.parseLong(innStr.substring(0,1))*3 +2;
                }else if(innStr.substring(2).equals("⅓")){
                    inn = Long.parseLong(innStr.substring(0,1))*3 +1;
                }
            }else{
                inn = Long.parseLong(innStr)*3;
            }
            Player player = playerService.getPlayerByName(name).get();
            Pitch pitch = (Pitch.builder().game(saveGame).clubId(15387L).player(player).result(pitchtable[i][1])
                    .inning(inn).batter(Long.parseLong(pitchtable[i][3])).hitter(Long.parseLong(pitchtable[i][4]))
                    .pHit(Long.parseLong(pitchtable[i][5]))).pHomerun(Long.parseLong(pitchtable[i][6]))
                    .sacrifice(Long.parseLong(pitchtable[i][7]))
                    .sacFly(Long.parseLong(pitchtable[i][8])).baseOnBall(Long.parseLong(pitchtable[i][9]))
                    .hitByBall(Long.parseLong(pitchtable[i][10])).stOut(Long.parseLong(pitchtable[i][11]))
                    .fallingBall(Long.parseLong(pitchtable[i][12])).balk(Long.parseLong(pitchtable[i][13]))
                    .lossScore(Long.parseLong(pitchtable[i][14])).selfLossScore(Long.parseLong(pitchtable[i][15]))
                    .build();
            pitchList.add(pitch);
        }
        pitchService.saveAll(pitchList);

        Elements cells3= pitchboardcells.select("table tbody tr td");
        Elements players3 = pitchboardcells.select("table tbody tr th");
        int numRows3 = pitchboardcells.select("table tbody tr").size();
        int numCols3 = pitchboardcells.select("table tbody tr:first-child td").size();
        String[][] table3 = new String[numRows3][numCols3];
        int iii = 0, jjj = 0;
        for (Element cell : cells3) {
            table3[iii][jjj++] = cell.text();
            if (jjj == numCols3) {
                iii++;
                jjj = 0;
            }
        }
        String[][] hittable3 = new String[numRows3][numCols3+1];
        for(int i=0; i<numRows3; i++){
            for(int j=1; j<numCols3; j++){
                hittable3[i][j] = table3[i][j-1];
            }
        }

        if(numRows3 < 9 || hittable3[0][1].isEmpty())
            return new ResponseDto("Success", 0);
        gameseq = 1L;
        pitcherBoardList.add(PitcherBoard.builder().build());
        int hitNo = 0;
        for(int j=1; j<12; j++){
            Queue<String> hitNoQ = new LinkedList<>();
            for(int i=0; i<numRows3; i++){
                if(!hittable3[hitNo][j].isEmpty()){
                    int cnt = hittable3[hitNo][j].split("/").length;
                    String hc = hittable3[hitNo][j].split("/")[0];
                    if(codeCache.getData(hc) == null)
                        continue;
                    PitcherBoard pitcherBoard = PitcherBoard.builder()
                            .game(saveGame).gameSeq(gameseq)
                            .hitCd(codeCache.getData(hc))
                            .hitNo((long) hitNo+1)
                            .inning((long) j)
                            .result(hc).build();
                    pitcherBoardList.set(Math.toIntExact(pitcherBoard.getGameSeq()), pitcherBoard);
                    if(cnt>1){
                        hitNoQ.add(hittable3[hitNo][j].split("/")[1]);
                    }
                    gameseq++;
                    hitNo++;
                    if(hitNo == 9)
                        hitNo = 0;
                }
            }
            while(!hitNoQ.isEmpty()){
                String hc = hitNoQ.poll();
                if(codeCache.getData(hc) == null)
                    continue;
                PitcherBoard pitcherBoard = PitcherBoard.builder()
                        .game(saveGame).gameSeq(gameseq)
                        .hitCd(codeCache.getData(hc))
                        .hitNo((long) hitNo+1)
                        .inning((long) j)
                        .result(hc).build();
                pitcherBoardList.set(Math.toIntExact(pitcherBoard.getGameSeq()), pitcherBoard);
                gameseq++;
                hitNo++;
                if(hitNo == 9)
                    hitNo = 0;
            }
        }

        int pitcherIdx = 0;
        int pitchStack = 0;
        if(pitchList.size() ==0)
            return new ResponseDto("Success", 0);
        Pitch pitch = pitchList.get(0);
        int pitcherSize = pitchList.size();
        for(int i=1; i<pitcherBoardList.size(); i++){
            if(pitchStack == 0){
                if(pitcherIdx >= pitcherSize) {
                    Player randomPlayer = playerService.getPlayerByName("강은비").get();
                    pitch = Pitch.builder().player(randomPlayer).batter(100L).build();
                }else
                    pitch = pitchList.get(pitcherIdx);
                pitchStack = Math.toIntExact(pitch.getBatter());
                pitcherIdx++;
            }
            pitcherBoardList.get(i).setPlayer(pitch.getPlayer());
            pitchStack--;
        }
        pitcherBoardList = new ArrayList<>(pitcherBoardList.subList(1, pitcherBoardList.size()));
        pitcherBoardService.saveAll(pitcherBoardList);

        return new ResponseDto("Success", 0);
    }
    @Transactional
    public void updateOp() throws IOException {
        List<Game> gameList = gameService.findAll();
        String urlForm = "http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=";
        for(Game game : gameList){

            if(game.getOpponent() == null){
                Connection conn = Jsoup.connect(urlForm+game.getId());
                Document document = conn.get();
                Elements scorebox = document.getElementsByClass("section_score");
                String fT = scorebox.get(0).child(0).select("dt").text();
                String lT = scorebox.get(0).child(1).select("dt").text();
                String scores =scorebox.get(0).getElementsByClass("score").text();
                Long homeScore = Long.parseLong(scores.split(" ")[0]);
                Long awayScore = Long.parseLong(scores.split(" ")[1]);
                String opponent;
                String result = "패";
                if(homeScore > awayScore){
                    if(fT.equals("팀 사야이"))
                        result = "승";
                }else if(homeScore < awayScore){
                    if(lT.equals("팀 사야이"))
                        result = "승";
                }else{
                    result = "무";
                }
                if(fT.equals("팀 사야이")) opponent=lT;
                else opponent=fT;

                game.setOpponent(opponent);
                game.setHomeScore(homeScore);
                game.setAwayScore(awayScore);
                game.setResult(result);
            }
        }
    }
    @Transactional
    public void updateSince(int year){
        for(int i=1; i<getPages(year)+1; i++){
            updateSince(year, i);
        }
    }
    @Transactional
    public void updateSince(int year, int page){
        String url = String.format("http://www.gameone.kr/club/info/schedule/table?club_idx=15387&season=%s&game_type=0&lig_idx=0&month=0&page=%s", year,page);
        Connection conn = Jsoup.connect(url);
        Document document = null;
        try {
            document = conn.get();
            //url의 내용을 HTML Document 객체로 가져온다.
            //https://jsoup.org/apidocs/org/jsoup/nodes/Document.html 참고
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements scorebox = document.getElementsByClass("game_table");
        Elements aHref = scorebox.select("table tbody tr td a");
        String urlForm ="http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=";
        for(Element ele : aHref){
            if(ele.hasClass("simbtn boxscore")){
                log.info("URL {}" , ele);
                String gameIdSplit = ele.toString().split(" ")[1].split(";game_idx=")[1];
                Long gameId = Long.parseLong(gameIdSplit.substring(0,gameIdSplit.length()-1));

                this.crawl(urlForm+gameId, Long.valueOf(year));
            }
        }
    }
    @Transactional
    public ResponseDto updateAllLeagueInfo(){
        String urlForm ="http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=";
        List<Game> gameList = gameService.findAll();
        for(Game g : gameList){
            Long season = g.getSeason();
            Long id = g.getId();
            String url = urlForm+id;
            Connection conn = Jsoup.connect(url);
            Document document = null;

            try {
                document = conn.get();
                //url의 내용을 HTML Document 객체로 가져온다.
                //https://jsoup.org/apidocs/org/jsoup/nodes/Document.html 참고
            } catch (IOException e) {
                return new ResponseDto("Jsoup Connection Error",10404);
            }
            Elements scorebox = document.getElementsByClass("section_score");
            String ltp = scorebox.get(0).getElementsByClass("score_teble").select("caption").text();
            String league = ltp.split("/")[0].trim();
            if(ltp.split("/").length>3){
                int len = ltp.split("/").length;
                league = Arrays.stream(ltp.split("/"),0,len-2)
                        .collect(Collectors.joining("/"));
            }

            Ligue ligue = ligueService.findByName(league, season)
                    .orElse(ligueService.findByName("원외리그", season).orElseThrow(NoSuchElementException::new));
            g.updateLeague(ligue.getId());
        }
        return ResponseDto.builder()
                .resultCode(0).resultMsg("Success").build();
    }
    private int getPages(int year){
        Map<Integer, Integer> pagelist = new HashMap<>();
        pagelist.put(2012, 1);
        pagelist.put(2013, 3);
        pagelist.put(2014, 2);
        pagelist.put(2015, 2);
        pagelist.put(2016, 3);
        pagelist.put(2017, 4);
        pagelist.put(2018, 4);
        pagelist.put(2019, 6);
        pagelist.put(2020, 8);
        pagelist.put(2021, 6);
        pagelist.put(2022, 6);
        pagelist.put(2023, 5);
        pagelist.put(2024, 5);
        pagelist.put(2025, 4);
        return pagelist.get(year);
    }
}
