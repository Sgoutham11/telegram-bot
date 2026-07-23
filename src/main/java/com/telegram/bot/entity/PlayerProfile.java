package com.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Entity
@Builder
@Table(name = "PLAYER_PROFILE")
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProfile {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_profile_seq")
    @SequenceGenerator(name = "player_profile_seq", sequenceName = "PLAYER_PROFILE_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "CHAT_ID", unique = true, nullable = false)
    private Long chatId;

    @Column(name = "PLAYER_NAME", nullable = false)
    private String playerName;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
