package com.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Entity
@Builder
@Table(name = "LOGIN_SECTION")
@NoArgsConstructor
@AllArgsConstructor
public class LoginSection {

    @Id
    @Column(name = "ID")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "ACCESS_TOKEN")
    private String accessToken;

    @Column(name = "PUBLIC_TOKEN")
    private String publicToken;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "REFRESH_TOKEN")
    private String refreshToken;

    @Column(name = "LOGIN_TIME")
    private Date loginTime;

    @Column(name = "REQUEST_DATE")
    private LocalDate requestDate;

    @Column(name = "USER_ID")
    private String userId;


}
