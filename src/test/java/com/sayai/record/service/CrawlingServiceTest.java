package com.sayai.record.service;

import com.sayai.record.model.Game;
import com.sayai.record.model.Ligue;
import com.sayai.record.model.enums.FirstLast;
import com.sayai.record.util.CodeCache;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class CrawlingServiceTest {
    private String testurl = "http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=1000308";
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
        //System.out.println(document.getElementsByClass("section_score"));
        List<String> list = new ArrayList<>();
        Elements scorebox = document.getElementsByClass("section_score");
        //System.out.println(scorebox);
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
                .gameTime(gametime).season((long) gamedate.getYear()).ligIdx(1L).opponent(opponent).build();

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
        //System.out.println(numRows);
        //System.out.println(numCols);
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
        //crawlingService.crawl("http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=331776");
    }
    @Test
    public void updateOppo() throws IOException {
        //crawlingService.updateOp();
    }

    @Test
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
}