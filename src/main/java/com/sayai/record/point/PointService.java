package com.sayai.record.point;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final MemberRepository memberRepository;

    public record RankingDto(int rank, Long memberId, String name, long balance) {}

    @Transactional(readOnly = true)
    public long getBalance(Long memberId) {
        return userPointRepository.findById(memberId).map(UserPoint::getBalance).orElse(0L);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getBalances(Collection<Long> memberIds) {
        return userPointRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(UserPoint::getMemberId, UserPoint::getBalance));
    }

    /** 잔액 순 랭킹. 동점은 같은 순위(1,1,3). 포인트 레코드 없는 멤버(0점)는 제외. */
    @Transactional(readOnly = true)
    public List<RankingDto> ranking(int limit) {
        List<UserPoint> points = userPointRepository.findAllByOrderByBalanceDescMemberIdAsc(PageRequest.of(0, limit));
        Map<Long, String> names = memberRepository.findAllById(points.stream().map(UserPoint::getMemberId).toList())
                .stream().collect(Collectors.toMap(Member::getMemberId, Member::getName));
        List<RankingDto> result = new ArrayList<>();
        int rank = 0;
        long prev = Long.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            UserPoint p = points.get(i);
            if (p.getBalance() != prev) { rank = i + 1; prev = p.getBalance(); }
            result.add(new RankingDto(rank, p.getMemberId(), names.getOrDefault(p.getMemberId(), "Unknown"), p.getBalance()));
        }
        return result;
    }

    /**
     * 포인트 증감 (delta > 0 증가, delta < 0 감소). 잔액 부족 시 IllegalArgumentException.
     * 다른 기능(베팅, 어드민 조정 등)에서만 호출한다. 호출자의 트랜잭션에 참여한다.
     */
    @Transactional
    public long adjust(Long memberId, long delta) {
        UserPoint point = userPointRepository.findForUpdate(memberId)
                .orElseGet(() -> userPointRepository.save(UserPoint.builder().memberId(memberId).balance(0).build()));
        point.adjust(delta);
        return point.getBalance();
    }
}
