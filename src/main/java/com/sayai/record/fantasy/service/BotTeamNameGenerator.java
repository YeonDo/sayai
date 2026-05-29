package com.sayai.record.fantasy.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotTeamNameGenerator {

    private static final List<String> PLACES = List.of(
            "청양", "단양", "보령", "태안", "홍성", "예산", "당진", "공주", "논산", "금산",
            "나주", "담양", "구례", "고흥", "영암", "함평", "남원", "무주", "고령", "의성",
            "청송", "영양", "영덕", "예천", "의령", "함안", "고성", "남해", "하동", "신안", "안산",
            "이천", "제주", "서귀포", "함평", "충주", "청주", "천안", "익산", "고양", "전주", "상동",
            "울산", "경산", "마산", "강화", "서산", "수지", "분당", "판교", "잠실", "송파", "성남", "전주",
            "용인", "군산", "오사카", "독도", "다낭", "울릉도", "동대문", "다산", "동작", "성동", "서대문", "재송",
            "서초", "김포", "마포", "관악", "용산", "거제", "청라", "양양", "속초", "목포", "안양", "아제로스", "필트오버"
    );

    private static final List<String> TEAM_SUFFIXES = List.of(
            "고릴라즈", "매드고트", "그리즐리", "기니피그", "가디언즈", "게이터스",
            "베이징덕", "돌핀즈", "레인디어", "떼껄룩즈", "다저스", "나이츠", "드림즈",
            "라이거즈", "라이노스", "라쿤즈", "래빗즈", "레오파드", "레이븐", "링크스", "리저드", "화이트삭스", "램스",
            "맨티스", "매그파이", "매머드", "핑크밍크", "글라스몽키",
            "바이슨", "바이퍼스", "데빌배트", "버터플라이", "버팔로즈", "마인벌처", "보아뱀즈", "비버즈", "블루윙스",
            "샤크스", "슬로스", "스콜피온", "스퀴드", "스팅레이", "씨호스",
            "아나콘다", "아울즈", "앨리게이", "옥토퍼스", "오르카", "씨오터", "우드페커", "울버린", "이구아나", "인디언스", "양키스",
            "재규어", "지라프", "와이번스", "유니콘스", "사우루스", "서퍼즈", "알바트로스",
            "치타즈", "산타클로스", "갈매기즈", "미니언즈",
            "카멜레온", "코모란트", "코브라", "코요테", "콘도르", "크툴루즈",
            "거북이즈", "웰시코기", "프로토스", "팰리컨스",
            "팔콘즈", "블랙팬서", "피라냐즈", "피콕스", "에코폭스", "포이즌프로그", "도플라밍고", "킬러판다", "이브이즈",
            "하이에나", "헤지호그", "호크스", "헝그리히포"
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
