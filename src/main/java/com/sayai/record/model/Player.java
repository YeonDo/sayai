package com.sayai.record.model;


import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerRecord;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "PLAYER")
@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLAYER_ID")
    private Long id;

    private Long backNo;

    private Long clubId;

    private String name;

    private String birth;

    private String finEdu;
    private String groupCode;
    private String sleepYn;
    @Builder.Default
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<Pitch> pitchList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<Hit> hitList = new ArrayList<>();


    public PlayerDto toDto(){
        return PlayerDto.builder()
                .id(this.id).backNo(this.backNo).name(this.name).build();
    }

}
