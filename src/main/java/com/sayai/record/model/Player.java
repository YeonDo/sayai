package com.sayai.record.model;


import lombok.*;

import javax.persistence.*;

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
}
