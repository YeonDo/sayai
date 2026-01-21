package com.sayai.record.service;

import com.sayai.record.dto.ResponseDto;
import com.sayai.record.model.*;
import com.sayai.record.model.enums.FirstLast;
import com.sayai.record.util.CodeCache;
import com.sayai.record.util.ExpandArrayList;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class CrawlingServiceTest {
    private String testurl = "http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=1579572";
    @Autowired
    private CodeCache codeCache;
    @Autowired
    private CrawlingService crawlingService;
    @Autowired
    private HitService hitService;
    @Autowired
    private GameService gameService;

    @Test
    void crawlMatch(){
        Connection conn = Jsoup.connect(testurl);
        Document document = null;
        try {
            document = conn.get();
            //url의 내용을 HTML Document 객체로 가져온다.
            //https://jsoup.org/apidocs/org/jsoup/nodes/Document.html 참고
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String,String> dataset = codeCache.getDataset();
        System.out.println(document.getElementsByClass("section_score"));
        List<String> list = new ArrayList<>();
        Elements scorebox = document.getElementsByClass("section_score");
        System.out.println(scorebox);
        System.out.println("--------------------------------------");
        System.out.println(scorebox.get(0).child(0).select("dt").text());
        System.out.println(scorebox.get(0).getElementsByClass("score").text());
        System.out.println(scorebox.get(0).getElementsByClass("score").text().split(" ")[1]);
        System.out.println("====================================");
        System.out.println(scorebox.get(0).child(1).select("dt").text());
        String ltp =scorebox.get(0).getElementsByClass("score_teble").select("caption").text();
        System.out.println(ltp);
        String fl = null;
        String fT = scorebox.get(0).child(0).select("dt").text();
        String lT = scorebox.get(0).child(1).select("dt").text();
        String opponent;
        String league = ltp.split("/")[0].trim();
        String time = ltp.split("/")[1].trim();
        String place = ltp.split("/")[2].trim();
        if(fT.equals("팀 사야이")){ fl = "F"; opponent = lT;}
        else {fl = "L"; opponent=fT;}
        int time_MM = Integer.parseInt(time.substring(0,2));
        int time_dd = Integer.parseInt(time.substring(3,5));
        int time_hh = Integer.parseInt(time.substring(6,8));
        int time_mm = Integer.parseInt(time.substring(9,11));
        LocalDate gamedate = LocalDate.of(2023,time_MM,time_dd);
        LocalTime gametime = LocalTime.of(time_hh,time_mm);
        Game game = Game.builder()
                .clubId(15387L).fl(FirstLast.valueOf(fl)).stadium(place).gameDate(gamedate)
                .gameTime(gametime).season((long) gamedate.getYear()).leagueId(1L).opponent(opponent).build();

        Elements record = document.getElementsByClass("record_table inc_round");
        //System.out.println(record.get(1));
        Elements precord = document.getElementsByClass("record_table");
        //System.out.println(precord.get(3));




// 테이블의 모든 셀을 선택합니다.
        Elements cells = record.get(1).select("table tbody tr td");
        //System.out.println(cells);
        Elements players = record.get(1).select("table tbody tr th");
// 테이블의 행 수와 열 수를 계산합니다.
        int numRows = record.get(1).select("table tbody tr").size();
        int numCols = record.get(1).select("table tbody tr:first-child td").size();
        System.out.println(numRows);
        System.out.println(numCols);
// 자바 행렬을 생성합니다.
        String[][] table = new String[numRows][numCols];
        String[] player = new String[numRows];
        System.out.println("-----------------------");
        //System.out.println(players);
        int k = 0;
        for(Element ele : players){
            player[k++] = ele.text();

        }
// 선택한 각 셀에서 텍스트를 추출하여 자바 행렬에 채웁니다.
        int i = 0, j = 0;
        for (Element cell : cells) {
            table[i][j++] = cell.text();

            if (j == numCols) {
                i++;
                j = 0;
            }
        }
        for(int ii=0; ii<numRows; ii++){
            for(int jj=0; jj<numCols; jj++){
                System.out.print(table[ii][jj] + " ");
            }
            System.out.println();
        }
        for(int ii=0; ii<numRows; ii++){
                System.out.println(player[ii].split(" ")[2].substring(0,3));
        }


        // pitch record

        int numRows2 = precord.get(2).select("table tbody tr").size();
        int numCols2 = precord.get(2).select("table tbody tr:first-child td").size();
        Elements cells2 = precord.get(2).select("table tbody tr td");
        //System.out.println(cells);
        Elements players2 = precord.get(2).select("table tbody tr th");
        String[][] table2 = new String[numRows][numCols];
        String[] player2 = new String[numRows];
        System.out.println("-----------------------");
        k = 0;
        for(Element ele : players2){
            player2[k++] = ele.text();

        }
// 선택한 각 셀에서 텍스트를 추출하여 자바 행렬에 채웁니다.
        i = 0;
        j = 0;
        for (Element cell : cells2) {
            table2[i][j++] = cell.text();

            if (j == numCols2) {
                i++;
                j = 0;
            }
        }
        for(int ii=0; ii<numRows2; ii++){
            for(int jj=0; jj<numCols2; jj++){
                System.out.print(table2[ii][jj] + " ");
            }
            System.out.println();
        }
        for(int ii=0; ii<numRows2; ii++){
            System.out.println(player2[ii].substring(0,3));
        }

    }

    @Test
    public void crawlTest2(){
        Connection conn = Jsoup.connect(testurl);
        Document document = null;
        try {
            document = conn.get();
            //url의 내용을 HTML Document 객체로 가져온다.
            //https://jsoup.org/apidocs/org/jsoup/nodes/Document.html 참고
        } catch (IOException e) {
            e.printStackTrace();
        }
        Long gameId = Long.parseLong(testurl.split("&")[1].split("=")[1]);
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


        int time_MM = Integer.parseInt(time.substring(0,2));
        int time_dd = Integer.parseInt(time.substring(3,5));
        int time_hh = Integer.parseInt(time.substring(6,8));
        int time_mm = Integer.parseInt(time.substring(9,11));
        LocalDate gamedate = LocalDate.of(2023,time_MM,time_dd);
        LocalTime gametime = LocalTime.of(time_hh,time_mm);
        Game game = Game.builder().id(gameId)
                .clubId(15387L).fl(FirstLast.valueOf(fl)).stadium(place).gameDate(gamedate)
                .gameTime(gametime).season((long) gamedate.getYear()).leagueId(1L).opponent(opponent)
                .homeScore(homeScore).awayScore(awayScore).result(result).build();



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
            System.out.println(name);
            Long hitseq = 1L;
            for(int j=1; j<12; j++){
                if(!hittable[i][j].isEmpty()){
                    int cnt = hittable[i][j].split("/").length;
                    for(int k=0; k<cnt; k++){
                        String hc = hittable[i][j].split("/")[k];
                        Hit hit = Hit.builder().gameSeq(gameseq).game(null).player(null).inning((long)j)
                                .hitNo(Long.parseLong(hittable[i][0].split(" ")[0]))
                                .hitSeq(hitseq).hitCd(codeCache.getData(hc))
                                .result(hc)
                                .build();
                        System.out.println(name + " " + hc + " " + codeCache.getData(hc));
                        hitList.add(hit);
                        hitseq++;
                        gameseq++;
                    }
                }
            }
            System.out.println(hittable[i][0].split(" ")[0]);
            System.out.println(hittable[i][13]);
            System.out.println(hittable[i][14]);
            System.out.println(hittable[i][15]);
            System.out.println(hittable[i][16]);
            System.out.println(hittable[i][17]);

        }

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
            Player player = Player.builder().name(name).build();
            Pitch pitch = (Pitch.builder().game(null).clubId(15387L).player(player).result(pitchtable[i][1])
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

        Elements cells3= pitchboardcells.select("table tbody tr td");
        Elements players3 = pitchboardcells.select("table tbody tr th");
        int numRows3 = pitchboardcells.select("table tbody tr").size();
        int numCols3 = pitchboardcells.select("table tbody tr:first-child td").size();
        String[][] table3 = new String[numRows3][numCols3];
        String[] player3= new String[numRows3];
        int kkk = 0;
        for(Element ele : players3){
            player3[kkk++] = ele.text().split(" ")[0]+ " "+ele.text().split(" ")[2].substring(0,3);
        }
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
            hittable3[i][0] = player3[i];
            for(int j=1; j<numCols3; j++){
                hittable3[i][j] = table3[i][j-1];
            }
        }

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
                            .game(null).gameSeq(gameseq)
                            .hitCd(codeCache.getData(hc).split(",")[0])
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
                PitcherBoard pitcherBoard = PitcherBoard.builder()
                        .game(null).gameSeq(gameseq)
                        .hitCd(codeCache.getData(hc).split(",")[0])
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
        Pitch pitch = pitchList.get(0);
        for(int i=1; i<pitcherBoardList.size(); i++){
            if(pitchStack == 0){
                pitch = pitchList.get(pitcherIdx);
                pitchStack = Math.toIntExact(pitch.getBatter());
                pitcherIdx++;
            }
            pitcherBoardList.get(i).setPlayer(pitch.getPlayer());
            pitchStack--;
        }
        pitcherBoardList = new ArrayList<>(pitcherBoardList.subList(1, pitcherBoardList.size()));
        for(PitcherBoard pitcherBoard : pitcherBoardList)
            System.out.println(pitcherBoard);
    }
    @Test
    public void updateOppo() throws IOException {
        //crawlingService.updateOp();
    }

    //@Test
    public void updateGame2012() throws IOException{
        String url = "http://www.gameone.kr/club/info/schedule/table?club_idx=15387&season=2012&game_type=0&lig_idx=0&month=0&page=1";
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
        //System.out.println(scorebox);
        Elements aHref = scorebox.select("table tbody tr td a");
        System.out.println(aHref.get(1));
        Element s = aHref.get(0);
        //System.out.println(s.hasClass("simbtn boxscore"));
        //System.out.println(aHref.get(1).hasClass("simbtn boxscore"));
        for(Element ele : aHref){
            if(ele.hasClass("simbtn boxscore")){
                System.out.println(ele.toString().split(" ")[1].split(";game_idx=")[1].substring(0,6));
            }
        }
    }
    @Test
    public void updateSinceTest() throws Exception{
        //crawlingService.updateSince(2013,2);
    }

    //@Test
    void updateAllLeagueInfo() {
        crawlingService.updateAllLeagueInfo();
    }
}