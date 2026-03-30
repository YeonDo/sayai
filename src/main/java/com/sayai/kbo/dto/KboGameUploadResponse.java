package com.sayai.kbo.dto;

import com.sayai.kbo.model.KboGame;
import com.sayai.kbo.model.KboHit;
import com.sayai.kbo.model.KboPitch;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KboGameUploadResponse {
    private KboGame game;
    private List<KboHit> hitters;
    private List<KboPitch> pitchers;
}
