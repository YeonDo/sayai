package com.sayai.record.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;

    @Transactional(readOnly = true)
    public long getBalance(Long memberId) {
        return userPointRepository.findById(memberId).map(UserPoint::getBalance).orElse(0L);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getBalances(Collection<Long> memberIds) {
        return userPointRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(UserPoint::getMemberId, UserPoint::getBalance));
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
