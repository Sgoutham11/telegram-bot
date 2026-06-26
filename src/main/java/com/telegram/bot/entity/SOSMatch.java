package com.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "SOS_MATCH")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SOSMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sos_match_seq")
    @SequenceGenerator(name = "sos_match_seq", sequenceName = "SOS_MATCH_SEQ", allocationSize = 1)
    @Column(name = "MATCH_ID")
    private Long matchId;

    @Column(name = "ROOM_CODE", length = 10)
    private String roomCode;

    @Column(name = "WINNER_TELEGRAM_ID")
    private Long winnerTelegramId;

    @Column(name = "GRID_SIZE")
    private Integer gridSize;

    @Column(name = "PLAYER_COUNT")
    private Integer playerCount;

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
