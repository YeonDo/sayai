package com.sayai.record.viewer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EpisodeDto {
    private int seq;
    private int number;
    private String title;
    private String link;
    private String[] tags;

}
