package com.sayai.record.fantasy.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotTeamNameGenerator {

    private static final List<String> PLACES = List.of(
            "청양", "단양", "보령", "태안", "홍성", "예산", "당진", "공주", "논산", "금산", "워터세븐",
            "나주", "담양", "구례", "고흥", "영암", "함평", "남원", "무주", "고령", "의성", "웨코문드", "나뭇잎마을",
            "청송", "영양", "영덕", "예천", "의령", "함안", "고성", "남해", "하동", "신안", "안산", "오산",
            "태초마을",
            "이천", "제주", "서귀포", "함평", "충주", "청주", "천안", "익산", "고양", "전주", "상동", "목동",
            "울산", "경산", "마산", "강화", "서산", "수지", "분당", "판교", "잠실", "송파", "성남", "전주", "페리온",
            "용인", "군산", "오사카", "독도", "다낭", "울릉도", "동대문", "다산", "동작", "성동", "서대문", "재송", "백룸",
            "서초", "김포", "마포", "관악", "용산", "거제", "청라", "양양", "속초", "목포", "안양", "아제로스", "필트오버"
    );

    private static final List<String> TEAM_SUFFIXES = List.of(
            "가디언즈", "갈매기즈", "거북이즈", "게이터스", "고릴라즈", "그리즐리", "글라스몽키", "기니피그", "고라니즈",
            "나이츠", "닌자어쌔신",
            "다저스", "데빌배트", "도플라밍고", "돌핀즈", "드림즈", "떼껄룩즈", "도깨비즈", "드럼통즈",
            "라이거즈", "라이노스", "라쿤즈", "래빗즈", "램스", "레오파드", "레이븐", "레인디어", "리저드", "링크스", "레이커스",
            "마인벌처", "매그파이", "매드고트", "매머드", "맨티스", "미니언즈",
            "바이슨", "바이퍼스", "버터플라이", "버팔로즈", "베이징덕", "보아뱀즈", "블랙팬서", "블루윙스", "비버즈", "블루머쉬룸",
            "사우루스", "산타클로스", "샤크스", "서퍼즈", "스콜피온", "스퀴드", "스팅레이", "슬로스", "씨오터", "씨호스", "스트릿캣츠", "스파이더", "사이버펑크",
            "아나콘다", "아울즈", "알바트로스", "앨리게이", "양키스", "에코폭스", "오르카", "옥토퍼스", "와이번스",
            "우드페커", "울버린", "웰시코기", "유니콘스", "이구아나", "이브이즈", "인디언스", "아카츠키",
            "재규어", "지라프",
            "치타즈", "치와와즈", "치즈와플", "칠리페퍼즈",
            "카멜레온", "코모란트", "코브라", "코요테", "콘도르", "크툴루즈", "킬러판다", "크로우즈",
            "튜나즈", "터틀즈",
            "팔콘즈", "팰리컨스", "포이즌프로그", "프로토스", "피라냐즈", "피콕스", "핑크밍크",
            "하이에나", "헝그리히포", "헤지호그", "호크스", "화이트삭스", "하이닉스"
    );

    public static final int MAX_COUNT = Math.min(PLACES.size(), TEAM_SUFFIXES.size());

    private BotTeamNameGenerator() {}

    /**
     * count개의 중복 없는 봇 팀명을 생성합니다. ("지명 팀명" 형식, 예: "청양 버팔로즈")
     *
     * @param count 생성할 팀명 수 (최대 {@link #MAX_COUNT}개)
     * @return 중복 없는 팀명 리스트
     * @throws IllegalArgumentException count가 최대 생성 가능 수를 초과하는 경우
     */
    public static List<String> generate(int count) {
        if (count > MAX_COUNT) {
            throw new IllegalArgumentException(
                    "생성 가능한 최대 팀명 수는 " + MAX_COUNT + "개입니다. 요청: " + count);
        }

        List<String> places = new ArrayList<>(PLACES);
        List<String> suffixes = new ArrayList<>(TEAM_SUFFIXES);
        Collections.shuffle(places);
        Collections.shuffle(suffixes);

        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(places.get(i) + " " + suffixes.get(i));
        }
        return result;
    }
}
