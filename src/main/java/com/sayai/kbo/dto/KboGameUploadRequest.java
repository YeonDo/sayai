package com.sayai.kbo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class KboGameUploadRequest {
    private Long season;
    private String home;
    private String away;
    private Long homeScore;
    private Long awayScore;
    private String result;
    private MultipartFile file;
}
