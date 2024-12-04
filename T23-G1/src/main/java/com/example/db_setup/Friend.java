package com.example.db_setup;

import lombok.Data;

import javax.persistence.*;

//Created by GabMan 04/12
@Table(name = "user_friend", schema = "studentsrepo") 
@Data
@Entity
public class Friend {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
//MAPPING NOMI DATABASE - NOMI CODICE
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "friend_id", nullable = false)
    private Integer friendId;

    @Column(name = "friend_username", nullable = false)
    private String friendUsername;

    @Column(name = "friend_avatar")
    private String friendAvatar;
}
