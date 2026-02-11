package com.sayai.record.auth.jwt;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {
    private final Long playerId;
    private final String name;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Long playerId, String name) {
        super(username, password, authorities);
        this.playerId = playerId;
        this.name = name;
    }
}
