package com.sayai.kbo.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class KboGameUploadRequest {
    private Long season;
    private String home;
    private String away;
    private Long homeScore;
    private Long awayScore;
    private String result;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate gameDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime gameTime;

    private MultipartFile file;
}
