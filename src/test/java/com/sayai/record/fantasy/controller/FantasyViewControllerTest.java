package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.service.FantasyGameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FantasyViewController.class)
class FantasyViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FantasyGameService fantasyGameService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @WithMockUser
    void dashboard_shouldRenderTemplate() throws Exception {
        mockMvc.perform(get("/fantasy"))
                .andExpect(status().isOk());
    }
}
