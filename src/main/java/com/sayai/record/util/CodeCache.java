package com.sayai.record.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CodeCache {
    private Map<String, String> dataset;
    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\s*\\[.*?\\]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    public CodeCache(){
        dataset = new HashMap<>();
        dataset.put("아웃", "0,0,0");
        dataset.put("삼진", "10,0,0");
        dataset.put("낫아웃-", "20,0,0");
        dataset.put("낫아웃+", "21,0,0");
        dataset.put("쓰리번트", "30,0,0");
        dataset.put("타자타구맞음", "40,0,0");
        dataset.put("수비방해", "50,0,0");
        dataset.put("부정타격", "60,0,0");
        dataset.put("주자아웃", "R0,0,0");
        dataset.put("도루자", "R10,0,0");
        dataset.put("견제사", "R20,0,0");
        dataset.put("런다운", "R30,0,0");
        dataset.put("타구맞음", "R40,0,0");
        dataset.put("수비방해", "R50,0,0");
        dataset.put("땅볼", "10H,0,0");
        dataset.put("투땅", "110,10H,0");
        dataset.put("포땅", "210,10H,0");
        dataset.put("1땅", "310,10H,0");
        dataset.put("2땅", "410,10H,0");
        dataset.put("3땅", "510,10H,0");
        dataset.put("유땅", "610,10H,0");
        dataset.put("좌땅", "710,10H,0");
        dataset.put("중땅", "810,10H,0");
        dataset.put("우땅", "910,10H,0");
        dataset.put("투땅R", "110R,10H,0");
        dataset.put("포땅R", "210R,10H,0");
        dataset.put("1땅R", "310R,10H,0");
        dataset.put("2땅R", "410R,10H,0");
        dataset.put("2직R", "420R,10H,0");
        dataset.put("3땅R", "510R,10H,0");
        dataset.put("유땅R", "610R,10H,0");
        dataset.put("좌땅R", "710R,10H,0");
        dataset.put("중땅R", "810R,10H,0");
        dataset.put("우땅R", "910R,10H,0");
        dataset.put("직선타", "20H,0,0");
        dataset.put("투직", "120,20H,0");
        dataset.put("1직", "320,20H,0");
        dataset.put("2직", "420,20H,0");
        dataset.put("3직", "520,20H,0");
        dataset.put("유직", "620,20H,0");
        dataset.put("플라이", "30H,0,0");

        dataset.put("투플", "130,30H,0");
        dataset.put("포플", "230,30H,0");
        dataset.put("1플", "330,30H,0");
        dataset.put("2플", "430,30H,0");
        dataset.put("3플", "530,30H,0");
        dataset.put("유플", "630,30H,0");
        dataset.put("좌플", "730,30H,0");
        dataset.put("중플", "830,30H,0");
        dataset.put("우플", "930,30H,0");
        dataset.put("번트", "0BH,0,0");
        dataset.put("투번", "110B,0BH,0");
        dataset.put("포번", "210B,0BH,0");
        dataset.put("1번", "310B,0BH,0");
        dataset.put("2번", "410B,0BH,0");
        dataset.put("3번", "510B,0BH,0");
        dataset.put("유번", "610B,0BH,0");
        dataset.put("투인플", "130I,30I,0");
        dataset.put("포인플", "230I,30I,0");
        dataset.put("1인플", "330I,30I,0");
        dataset.put("2인플", "430I,30I,0");
        dataset.put("3인플", "530I,30I,0");
        dataset.put("유인플", "630I,30I,0");

        dataset.put("파플", "30F,0,0");
        dataset.put("투파플", "130F,30F,0");
        dataset.put("포파플", "230F,30F,0");
        dataset.put("1파플", "330F,30F,0");
        dataset.put("2파플", "430F,30F,0");
        dataset.put("3파플", "530F,30F,0");
        dataset.put("유파플", "630F,30F,0");
        dataset.put("좌파플", "730F,30F,0");
        dataset.put("중파플", "830F,30F,0");
        dataset.put("우파플", "930F,30F,0");
        dataset.put("병살", "00,0,0");
        dataset.put("투땅병살", "1100,00,0");
        dataset.put("포땅병살","2100,00,0");
        dataset.put("1땅병살", "3100,00,0");
        dataset.put("2땅병살", "4100,00,0");
        dataset.put("3땅병살", "5100,00,0");
        dataset.put("유땅병살", "6100,00,0");
        dataset.put("투직병살", "1200,00,0");
        dataset.put("1직병살", "3200,00,0");
        dataset.put("2직병살", "4200,00,0");
        dataset.put("3직병살", "5200,00,0");
        dataset.put("유직병살", "6200,00,0");

        dataset.put("희생", "8,null,8");
        dataset.put("희플", "38,8,8");
        dataset.put("1희플", "338,38,8");
        dataset.put("2희플", "438,38,8");
        dataset.put("3희플", "538,38,8");
        dataset.put("유희플", "638,38,8");
        dataset.put("좌희플", "738,38,8");
        dataset.put("중희플", "838,38,8");
        dataset.put("우희플", "938,38,8");
        dataset.put("파희플", "38F,8,8");
        dataset.put("1파희플", "338F,8,8");
        dataset.put("2파희플", "438F,38F,8");
        dataset.put("3파희플", "538F,38F,8");
        dataset.put("유파희플", "638F,38F,8");
        dataset.put("좌파희플", "738F,38F,8");
        dataset.put("우파희플", "938F,38F,8");

        dataset.put("희번", "8B,8,8");
        dataset.put("투희번", "118B,8B,8");
        dataset.put("포희번", "218B,8B,8");
        dataset.put("1희번", "318B,8B,8");
        dataset.put("2희번", "418B,8B,8");
        dataset.put("3희번", "518B,8B,8");
        dataset.put("유희번", "618B,8B,8");
        dataset.put("희출", "87,8,8");
        dataset.put("1희플출", "3587,87,8");
        dataset.put("2희플출", "4587,87,8");
        dataset.put("3희플출", "5587,87,8");
        dataset.put("유희플출", "6587,87,8");
        dataset.put("좌희플출", "7587,87,8");
        dataset.put("중희플출", "8587,87,8");
        dataset.put("우희플출", "9587,87,8");

        dataset.put("희번출", "87B,8,8");
        dataset.put("투희번출", "1587B,87B,8");
        dataset.put("포희번출", "2587B,87B,8");
        dataset.put("1희번출", "3587B,87B,8");
        dataset.put("2희번출", "4587B,87B,8");
        dataset.put("3희번출", "5587B,87B,8");
        dataset.put("유희번출", "6587B,87B,8");

        dataset.put("1루타", "1,null,1");
        dataset.put("투안", "151,1,1");
        dataset.put("포안", "251,1,1");
        dataset.put("1내안", "351,1,1");
        dataset.put("2내안", "451,1,1");
        dataset.put("3내안", "551,1,1");
        dataset.put("유내안", "651,1,1");
        dataset.put("좌안", "751,1,1");
        dataset.put("좌중안", "781,1,1");
        dataset.put("좌전안", "751A,1,1");
        dataset.put("좌선안", "751B,1,1");
        dataset.put("좌월안", "751C,1,1");
        dataset.put("중안", "851,1,1");
        dataset.put("중전안", "851A,1,1");
        dataset.put("중월안", "851B,1,1");
        dataset.put("우중안", "891,1,1");
        dataset.put("우안", "951,1,1");
        dataset.put("우전안", "951A,1,1");
        dataset.put("우선안", "951B,1,1");
        dataset.put("우월안", "951C,1,1");
        dataset.put("번안", "1B,1,1");
        dataset.put("투번안", "151B,1,1");
        dataset.put("포번안", "251B,1,1");
        dataset.put("1번안", "351B,1,1");
        dataset.put("2번안", "451B,1,1");
        dataset.put("3번안", "551B,1,1");
        dataset.put("유번안", "651B,1,1");

        dataset.put("2루타", "2,null,2");
        dataset.put("유내안2", "652,2,2");
        dataset.put("좌선2", "772,2,2");
        dataset.put("좌전2", "722,2,2");
        dataset.put("좌월2", "782,2,2");
        dataset.put("좌안2", "712,2,2");
        dataset.put("좌중2", "872,2,2");
        dataset.put("중전2", "822,2,2");
        dataset.put("중월2", "882,2,2");
        dataset.put("중안2", "882A,2,2");
        dataset.put("우중2", "892,2,2");
        dataset.put("우전2", "922,2,2");
        dataset.put("우안2", "912,2,2");
        dataset.put("우월2", "982,2,2");
        dataset.put("우선2", "992,2,2");
        dataset.put("인정2", "2A,2,2");
        dataset.put("인좌선2", "772A,2A,2");
        dataset.put("인좌월2", "782B,2A,2");
        dataset.put("인좌중2", "872A,2A,2");
        dataset.put("인우주2", "892A,2A,2");
        dataset.put("인우월2", "982A,2A,2");
        dataset.put("인우선2", "992A,2A,2");

        dataset.put("3루타", "3,null,3");
        dataset.put("좌안3", "713,3,3");
        dataset.put("좌선3", "773,3,3");
        dataset.put("좌전3", "723,3,3");
        dataset.put("좌월3", "783,3,3");
        dataset.put("좌중3", "873,3,3");
        dataset.put("중정3", "823,3,3");
        dataset.put("중월3", "883,3,3");
        dataset.put("중안3", "883A,3,3");
        dataset.put("중전3", "883B,3,3");
        dataset.put("우중3", "893,3,3");
        dataset.put("우안3", "913,3,3");
        dataset.put("우전3", "923,3,3");
        dataset.put("우월3", "983,3,3");
        dataset.put("우선3", "993,3,3");

        dataset.put("홈런", "4,null,4");
        dataset.put("좌선홈", "774,4,4");
        dataset.put("좌월홈", "784,4,4");
        dataset.put("좌중홈", "874,4,4");
        dataset.put("중월홈", "884,4,4");
        dataset.put("우중홈", "894,4,4");
        dataset.put("우월홈", "984,4,4");
        dataset.put("우선홈", "994,4,4");
        dataset.put("G홈", "4G,4,4");
        dataset.put("좌선G홈", "774G,4G,4");
        dataset.put("좌월G홈", "784G,4G,4");
        dataset.put("좌중G홈", "874G,4G,4");
        dataset.put("중월G홈", "884G,4G,4");
        dataset.put("우중G홈", "894G,4G,4");
        dataset.put("우월G홈", "984G,4G,4");
        dataset.put("우선G홈", "994G,4G,4");

        dataset.put("실책", "5,null,5");
        dataset.put("투실", "155,5,5");
        dataset.put("투플실", "155A,5,5");
        dataset.put("포실", "255,5,5");
        dataset.put("1실", "355,5,5");
        dataset.put("1플실", "355A,5,5");
        dataset.put("2실", "455,5,5");
        dataset.put("2땅실", "455A,5,5");
        dataset.put("2플실", "455B,5,5");
        dataset.put("3실", "555,5,5");
        dataset.put("3땅실", "555A,5,5");
        dataset.put("3플실", "555B,5,5");
        dataset.put("유실", "655,5,5");
        dataset.put("유땅실", "655A,5,5");
        dataset.put("유직실", "655B,5,5");
        dataset.put("유플실", "655C,5,5");
        dataset.put("좌실", "755,5,5");
        dataset.put("중실", "855,5,5");
        dataset.put("우실", "955,5,5");
        dataset.put("좌플실", "755A,5,5");
        dataset.put("중플실", "855A,5,5");
        dataset.put("우플실", "955A,5,5");

        dataset.put("야선", "6,6,5");
        dataset.put("투야선", "156,6,5");
        dataset.put("포야선", "256,6,5");
        dataset.put("1야선", "356,6,5");
        dataset.put("2야선", "456,6,5");
        dataset.put("3야선", "556,6,5");
        dataset.put("유야선", "656,6,5");

        dataset.put("출루", "7,null,7");
        dataset.put("사구", "22,7,7");
        dataset.put("고의4구", "31,7,7");
        dataset.put("4구", "41,7,7");
        dataset.put("타격방해", "51,7,7");
        dataset.put("승부주자", "77,7,7");

    }
    public String getData(String key){
        String[] hcSplit = key.split(",");
        int size = hcSplit.length;
        for(int i=0; i<size; i++){
            String hc = cleanString(hcSplit[i]);
            if(dataset.containsKey(hc))
                return dataset.get(hc).split(",")[0];
            if(hc.equals("대주자") || hc.equals("대수비") || hc.equals("대타")){
                return null;
            }
        }
        return "check";
    }
    public Map<String, String> getDataset(){
        return dataset;
    }
    public static String cleanString(String input){
        if(input == null)
            return null;

        String noBracket = BRACKET_PATTERN.matcher(input).replaceAll("");
        String noSpaces = WHITESPACE_PATTERN.matcher(noBracket).replaceAll("");
        return noSpaces;
    }
}
