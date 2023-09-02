package com.sayai.record.controller;


import com.sayai.record.dto.LigueRecord;
import com.sayai.record.dto.ResponseDto;
import com.sayai.record.service.LigueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController("/apis/v1/league")
@RequiredArgsConstructor
public class LigueController {
    private final LigueService ligueService;

    @PostMapping
    @ResponseBody
    public ResponseDto addLeague(@RequestBody LigueRecord input){
        ligueService.saveLeague(input);
        return new ResponseDto().builder().resultCode(0).resultMsg("Success").build();
    }
}
