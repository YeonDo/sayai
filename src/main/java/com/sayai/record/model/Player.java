package com.sayai.record.model;


import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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

    private Long back_no;

    private Long clubId;

    private String name;

    private String birth;

    private String finEdu;
    private String groupCode;
    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pitch> pitchList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    private List<Hit> hitList = new ArrayList<>();

}
