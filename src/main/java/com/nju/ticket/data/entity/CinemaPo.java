package com.nju.ticket.data.entity;

import javax.persistence.*;
import java.util.List;

/**
 * Created by sbin on 2017/6/8.
 */
@Entity
@Table(name = "cinema")
public class CinemaPo {

    @Id
    @GeneratedValue
    @Column
    int id;

    @Column
    String name;

    @Column
    String location;

    @Column
    String shortName;

    @OneToMany(mappedBy = "cinema")
    List<ScreeningPo> screeningList;

}