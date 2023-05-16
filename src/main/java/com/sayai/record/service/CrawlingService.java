package com.sayai.record.service;

import com.sayai.record.model.*;
import com.sayai.record.model.enums.FirstLast;
import com.sayai.record.repository.*;
import com.sayai.record.util.CodeCache;
import lombok.AllArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
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
        Map<String,String> dataset = codeCache.getDataset();
        try {
            document = conn.get();
            //url의 내용을 HTML Document 객체로 가져온다.
            //https://jsoup.org/apidocs/org/jsoup/nodes/Document.html 참고
        } catch (IOException e) {
            e.printStackTrace();
        }
        Long gameId = Long.parseLong(url.split("&")[1].split("=")[1]);
        List<String> list = new ArrayList<>();
        String fl = null;
        Elements scorebox = document.getElementsByClass("section_score");
        String fT = scorebox.get(0).child(0).select("dt").text();
        String lT = scorebox.get(0).child(1).select("dt").text();
        String opponent;
        String ltp = scorebox.get(0).getElementsByClass("score_teble").select("caption").text();
        String league = ltp.split("/")[0].trim();
        String time = ltp.split("/")[1].trim();
        String place = ltp.split("/")[2].trim();
        if(fT.equals("팀 사야이")){ fl = "F"; opponent = lT;}
        else {fl = "L"; opponent=fT;}
        Ligue lig = ligueService.findByName(league).get();
        int time_MM = Integer.parseInt(time.substring(0,2));
        int time_dd = Integer.parseInt(time.substring(3,5));
        int time_hh = Integer.parseInt(time.substring(6,8));
        int time_mm = Integer.parseInt(time.substring(9,11));
        LocalDate gamedate = LocalDate.of(2023,time_MM,time_dd);
        LocalTime gametime = LocalTime.of(time_hh,time_mm);
        Game game = Game.builder().id(gameId)
                .clubId(15387L).fl(FirstLast.valueOf(fl)).stadium(place).gameDate(gamedate)
                .gameTime(gametime).season((long) gamedate.getYear()).ligIdx(lig.getId()).opponent(opponent).build();
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
            player1[kk++] = ele.text().split(" ")[2].substring(0,3);
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
            String name = hittable[i][0];
            Player player = playerService.getPlayerByName(name).get();
            for(int j=1; j<12; j++){
                if(!hittable[i][j].isEmpty()){
                    Hit hit = Hit.builder().gameSeq(gameseq).game(saveGame).player(player).inning((long)j)
                            .hitNo((long)(i+1)).hitSeq((long)i).hitCd(dataset.get(hittable[i][j]).split(",")[0]).result(hittable[i][j])
                            .build();
                    hitList.add(hit);
                    gameseq++;
                }
            }
        }
        hitService.saveAll(hitList);

        Elements cells2= pitchcells.select("table tbody tr td");
        Elements players2 = pitchcells.select("table tbody tr th");
        int numRows2 = pitchcells.select("table tbody tr").size();
        int numCols2 = pitchcells.select("table tbody tr:first-child td").size();
        String[][] table2 = new String[numRows1][numCols1];
        String[] player2= new String[numRows1];
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
            String innStr = hittable[i][2];
            Long inn;
            if(innStr.length()>=3){
                long l = Long.parseLong(innStr.substring(0, 1)) * 3;
                if(innStr.substring(2).equals("⅔")){
                    inn = l +2;
                }else if(innStr.substring(2).equals("⅓")){
                    inn = l +1;
                }
            }else{
                inn = Long.parseLong(innStr)*3;
            }
            Player player = playerService.getPlayerByName(name).get();
            Pitch pitch = (Pitch.builder().game(saveGame).clubId(15387L).player(player).result(pitchtable[i][1])
                    .inning(0L).batter(Long.parseLong(pitchtable[i][3])).hitter(Long.parseLong(pitchtable[i][4]))
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


}
