package com.sayai.record.auth.entity;

import com.sayai.record.auth.repository.MemberRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

// Helper to bridge Member repository
public class MemberHelper {
    public static Map<Long, String> getNames(MemberRepository repo, Collection<Long> ids) {
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getPlayerId, Member::getName));
    }
}
