package com.sayai.kbo.service;

public class PRankWeight {

    // 타자 가중치 (a * stat + b)
    public static final double HITTER_AVG_A  = 775.75;
    public static final double HITTER_AVG_B  = -147.89;
    public static final double HITTER_HR_A   = 8.83;
    public static final double HITTER_HR_B   = 22.34;
    public static final double HITTER_RBI_A  = 2.13;
    public static final double HITTER_RBI_B  = 12.07;
    public static final double HITTER_SB_A   = 13.0;
    public static final double HITTER_SB_B   = 15.74;
    public static final double HITTER_SO_A   = -2.5;
    public static final double HITTER_SO_B   = 135.69;

    // 투수 가중치 (a * stat + b)
    public static final double PITCHER_WIN_A   = 17.83;
    public static final double PITCHER_WIN_B   = 17.56;
    public static final double PITCHER_ERA_A   = -14.84;
    public static final double PITCHER_ERA_B   = 120.79;
    public static final double PITCHER_SO_A    = 2.76;
    public static final double PITCHER_SO_B    = -35.98;
    public static final double PITCHER_WHIP_A  = -88.71;
    public static final double PITCHER_WHIP_B  = 185.47;
    public static final double PITCHER_SAVE_A  = 24.96;
    public static final double PITCHER_SAVE_B  = 27.04;

    private PRankWeight() {}
}
