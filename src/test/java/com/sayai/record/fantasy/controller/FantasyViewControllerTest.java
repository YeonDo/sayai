package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.jwt.JwtTokenProvider;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.service.FantasyGameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FantasyViewController.class)
class FantasyViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FantasyGameService fantasyGameService;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "user1")
    void testDraftRoomWithAuthenticatedUser() throws Exception {
        // Given
        Long playerId = 100L;
        Long gameSeq = 1L;
        Long draftingGameSeq = 1L;

        Member member = Member.builder()
                .playerId(playerId)
                .userId("user1")
                .name("Test User")
                .password("password")
                .role(Member.Role.USER)
                .build();

        given(memberRepository.findByUserId("user1")).willReturn(Optional.of(member));
        given(fantasyGameService.findDraftingGameId(playerId)).willReturn(draftingGameSeq);

        // When & Then
        mockMvc.perform(get("/fantasy/draft/{gameSeq}", gameSeq))
                .andExpect(status().isOk())
                .andExpect(view().name("fantasy/draft"))
                .andExpect(model().attribute("gameSeq", gameSeq))
                .andExpect(model().attribute("currentUserId", playerId))
                .andExpect(model().attribute("draftingGameSeq", draftingGameSeq));
    }
}
