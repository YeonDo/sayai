package com.sayai.record.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sayai.record.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.sayai.record.model.QPlayer.player;

@SpringBootTest
public class querydslTest {
    @Autowired
    JPAQueryFactory jpaQueryFactory;

    @Test
    void select(){
        Player result1 = jpaQueryFactory.select(player)
                .from(player).fetchFirst();
        System.out.println(result1.getName());
    }
}
