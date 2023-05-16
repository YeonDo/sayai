package com.sayai.record.model;


import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Table(name = "LIG")
public class Ligue {

    @Id
    @Column(name = "LIG_IDX")
    private Long id;

    private Long clubId;

    private Long season;

    private Long bucode;

    private Long jocode;

    private Long gameRule;
    @Column(name = "LIG_NAME")
    private String name;
}
