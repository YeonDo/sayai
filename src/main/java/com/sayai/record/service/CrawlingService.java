package com.sayai.record.service;

import com.sayai.record.model.*;
import com.sayai.record.model.enums.FirstLast;
import com.sayai.record.repository.*;
import com.sayai.record.util.CodeCache;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class CrawlingService {
    private final PlayerService playerService;
    private final LigueService ligueService;
    private final HitService hitService;
    private final PitchService pitchService;
    private final GameService gameService;
    private final CodeCache codeCache;
    public void crawl(String url){
        Connection conn = Jsoup.connect(url);
        Document document = null;
        System.out.println(url);
        try {
            document = conn.get();
            //url의 내용을 HTML Document 객체로 가져온다.
            //https://jsoup.org/apidocs/org/jsoup/nodes/Document.html 참고
        } catch (IOException e) {
            e.printStackTrace();
        }
        Long gameId = Long.parseLong(url.split("&")[1].split("=")[1]);
        Optional<Game> gameSearched = gameService.findGame(gameId);
        gameSearched.ifPresent(game -> {
            throw new RuntimeException("Game Already Exists");
        });
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
        if(fT.equals("팀 사야이")){ fl = "F"; opponent = lT;}
        else {fl = "L"; opponent=fT;}
        Optional<Ligue> byName = ligueService.findByName(league);
        Long ligId = byName.map(Ligue::getId).orElseGet(()->2L);
        int time_MM = Integer.parseInt(time.substring(0,2));
        int time_dd = Integer.parseInt(time.substring(3,5));
        int time_hh = Integer.parseInt(time.substring(6,8));
        int time_mm = Integer.parseInt(time.substring(9,11));
        LocalDate gamedate = LocalDate.of(2023,time_MM,time_dd);
        LocalTime gametime = LocalTime.of(time_hh,time_mm);
        Game game = Game.builder().id(gameId)
                .clubId(15387L).fl(FirstLast.valueOf(fl)).stadium(place).gameDate(gamedate)
                .gameTime(gametime).season((long) gamedate.getYear()).ligIdx(ligId).opponent(opponent)
                .homeScore(homeScore).awayScore(awayScore).result(result).build();
        Game saveGame = gameService.saveGame(game);


        List<Hit> hitList = new ArrayList<>();
        List<Pitch> pitchList = new ArrayList<>();
        Elements record = document.getElementsByClass("record_table");
        Element hitcells;
        Element pitchcells;
        if(fl.equals("F")){
            hitcells = record.get(0);
            pitchcells = record.get(2);
        }else{
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
            System.out.println(name);
            Player player = playerService.getPlayerByName(name).get();
            Long hitseq = 1L;
            for(int j=1; j<12; j++){
                if(!hittable[i][j].isEmpty()){
                    int cnt = hittable[i][j].split("/").length;
                    for(int k=0; k<cnt; k++){
                        String hc = hittable[i][j].split("/")[k].split(",")[0];
                        if(codeCache.getData(hc) == null)
                            continue;
                        Hit hit = Hit.builder().gameSeq(gameseq).game(saveGame).player(player).inning((long)j)
                                .hitNo(Long.parseLong(hittable[i][0].split(" ")[0]))
                                .hitSeq(hitseq).hitCd(codeCache.getData(hc).split(",")[0])
                                .result(hc)
                                .build();
                        hitList.add(hit);
                        hitseq++;
                        gameseq++;
                    }
                }
            }
        }
        hitService.saveAll(hitList);

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
            System.out.println(name);
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
    }
    @Transactional
    public void updateOp() throws IOException {
        List<Game> gameList = gameService.findAll();
        String urlForm = "http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=";
        for(Game game : gameList){
            System.out.println("---------------------");
            if(game.getOpponent() == null){
                System.out.println(game.getId());
                System.out.println(game.getGameDate());
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
                System.out.println(opponent);
                game.setOpponent(opponent);
                game.setHomeScore(homeScore);
                game.setAwayScore(awayScore);
                game.setResult(result);
            }
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
        HashSet<Long> set = new HashSet<>();
        String urlForm ="http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=";
        for(Element ele : aHref){
            if(ele.hasClass("simbtn boxscore")){
                Long gameId = Long.parseLong(ele.toString().split(" ")[1].split(";game_idx=")[1].substring(0, 6));
                System.out.println("gameId : " + gameId);
                this.crawl(urlForm+gameId);
            }
        }
    }

}
